package no.nav.k9.sak.domene.person.pdl;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static no.nav.k9.felles.integrasjon.pdl.IdentGruppe.AKTORID;
import static org.jboss.weld.util.collections.ImmutableList.of;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkResult;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterBolkResultResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.HentIdenterQueryRequest;
import no.nav.k9.felles.integrasjon.pdl.IdentGruppe;
import no.nav.k9.felles.integrasjon.pdl.IdentInformasjon;
import no.nav.k9.felles.integrasjon.pdl.IdentInformasjonResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.IdentlisteResponseProjection;
import no.nav.k9.felles.integrasjon.pdl.PdlKlient;
import no.nav.k9.felles.util.LRUCache;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

@ApplicationScoped
public class AktørTjeneste {
    private static final Logger log = LoggerFactory.getLogger(AktørTjeneste.class);
    private static final int DEFAULT_CACHE_SIZE = 1000;
    private static final long DEFAULT_CACHE_TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);

    private LRUCache<AktørId, PersonIdent> cacheAktørIdTilIdent;
    private LRUCache<PersonIdent, AktørId> cacheIdentTilAktørId;

    private PdlKlient pdlKlient;

    AktørTjeneste() {
        //
    }

    @Inject
    public AktørTjeneste(PdlKlient pdlKlient) {
        this.pdlKlient = pdlKlient;
        this.cacheAktørIdTilIdent = new LRUCache<>(DEFAULT_CACHE_SIZE, DEFAULT_CACHE_TIMEOUT);
        this.cacheIdentTilAktørId = new LRUCache<>(DEFAULT_CACHE_SIZE, DEFAULT_CACHE_TIMEOUT);
    }

    public Optional<AktørId> hentAktørIdForPersonIdent(PersonIdent personIdent) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }

        var aktørIdFraCache = ofNullable(cacheIdentTilAktørId.get(personIdent));
        if (aktørIdFraCache.isPresent()) {
            return aktørIdFraCache;
        }

        Optional<AktørId> aktørId = hentAktørIdFraPDL(personIdent.getIdent());
        aktørId.ifPresent(aid -> {
            // Kan ikke legge til i cache aktørId -> ident ettersom ident kan være ikke-current
            cacheIdentTilAktørId.put(personIdent, aid);
        });
        return aktørId;
    }

    public Optional<PersonIdent> hentPersonIdentForAktørId(AktørId aktørId) {
        var personIdentFraCache = ofNullable(cacheAktørIdTilIdent.get(aktørId));
        if (personIdentFraCache.isPresent()) {
            return personIdentFraCache;
        }

        Optional<PersonIdent> personIdent = hentPersonIdentFraPDL(aktørId.getId());
        personIdent.ifPresentOrElse(
            i -> {
                cacheAktørIdTilIdent.put(aktørId, i);
                cacheIdentTilAktørId.put(i, aktørId); // OK her, men ikke over ettersom dette er gjeldende mapping
            }, () -> log.info("Uventet resultat fra PDL: Fant ikke person med oppgitt aktørId."));
        return personIdent;
    }

    /** returnerer map av aktørId->personident (null dersom ikke funnet). */
    public Map<AktørId, PersonIdent> hentPersonIdentForAktørIder(Set<AktørId> aktørIder) {
        var query = new HentIdenterBolkQueryRequest();
        query.setIdenter(aktørIder.stream().map(AktørId::getId).collect(toList()));
        query.setGrupper(of(IdentGruppe.FOLKEREGISTERIDENT));

        var projection = new HentIdenterBolkResultResponseProjection()
            .ident()
            .identer(new IdentInformasjonResponseProjection()
                .ident()
                .gruppe())
            .code();

        Predicate<IdentInformasjon> erØnsketIdentgruppe = identInformasjon -> identInformasjon.getGruppe().equals(IdentGruppe.FOLKEREGISTERIDENT);

        var results = new TreeMap<AktørId, PersonIdent>();
        aktørIder.stream().forEach(a -> results.put(a, null));

        var bolkResults = pdlKlient.hentIdenterBolkResults(query, projection);
        log.info("Fant {} resultater", bolkResults == null ? 0 : bolkResults.size());
        var map = bolkResults.stream()
            .filter(r -> r.getCode() == null || Objects.equals(r.getCode(), "ok"))
            .filter(r -> r.getIdenter() != null && r.getIdenter().stream().anyMatch(erØnsketIdentgruppe))
            .collect(Collectors.toMap(
                r -> new AktørId(r.getIdent()),
                r -> new PersonIdent(r.getIdenter().stream().filter(erØnsketIdentgruppe).findAny().get().getIdent())));

        var feilende = bolkResults.stream()
            .filter(r -> r.getCode() != null && !Objects.equals(r.getCode(), "ok"))
            .collect(Collectors.groupingBy(HentIdenterBolkResult::getCode, Collectors.counting()));

        log.info("Forespurt [{}] identer, Hentet [{}] identer. Savner [{}]", aktørIder.size(), bolkResults.size(), feilende.size());

        results.putAll(map);
        return Collections.unmodifiableMap(results);
    }

    public Set<AktørId> hentAktørIdForPersonIdentSet(Set<PersonIdent> personIdentSet) {
        var personIdentIkkeICache = personIdentSet.stream()
            .filter(pid -> ofNullable(cacheIdentTilAktørId.get(pid)).isEmpty())
            .collect(toList());

        return concat(
            personIdentSet.stream()
                .map(pid -> ofNullable(cacheIdentTilAktørId.get(pid)))
                .flatMap(Optional::stream),
            personIdentIkkeICache.isEmpty() ? empty()
                : hentBolkMedAktørId(personIdentIkkeICache)
                    .peek(aktørInfo -> cacheIdentTilAktørId.put(aktørInfo.getElement1(), aktørInfo.getElement2()))
                    .map(Tuple::getElement2))
                        .collect(toSet());
    }

    private Stream<Tuple<PersonIdent, AktørId>> hentBolkMedAktørId(List<PersonIdent> personIdents) {
        var query = new HentIdenterBolkQueryRequest();
        var fødselsnummere = personIdents.stream().map(PersonIdent::getIdent).collect(toList());
        if (fødselsnummere.contains(null)) {
            throw new IllegalArgumentException("Kan ikke søke på ident som ikke er gyldig fnr. Mulig feil er at fnr" +
                "har feil lengde, har feil sjekksum, eller er et FDAT-nummer");
        }
        query.setIdenter(fødselsnummere);
        query.setGrupper(of(IdentGruppe.AKTORID));

        var projection = new HentIdenterBolkResultResponseProjection()
            .ident()
            .identer(new IdentInformasjonResponseProjection()
                .ident()
                .gruppe())
            .code();

        Predicate<IdentInformasjon> erØnsketIdentgruppe = identInformasjon -> identInformasjon.getGruppe().equals(IdentGruppe.AKTORID);

        // noinspection OptionalGetWithoutIsPresent
        return pdlKlient.hentIdenterBolkResults(query, projection).stream()
            .filter(r -> r.getIdenter().stream().anyMatch(erØnsketIdentgruppe))
            .map(r -> new Tuple<>(new PersonIdent(r.getIdent()), new AktørId(r.getIdenter().stream().filter(erØnsketIdentgruppe).findAny().get().getIdent())));
    }

    private Optional<AktørId> hentAktørIdFraPDL(String personIdent) {
        return identerFor(personIdent, AKTORID).map(IdentInformasjon::getIdent).map(AktørId::new);
    }

    private Optional<PersonIdent> hentPersonIdentFraPDL(String aktørId) {
        return identerFor(aktørId, IdentGruppe.FOLKEREGISTERIDENT).map(IdentInformasjon::getIdent).map(PersonIdent::new);

    }

    private Optional<IdentInformasjon> identerFor(String ident, IdentGruppe identGruppe) {
        var request = new HentIdenterQueryRequest();
        request.setIdent(ident);
        request.setGrupper(List.of(identGruppe));
        request.setHistorikk(Boolean.FALSE);

        IdentlisteResponseProjection projeksjon = new IdentlisteResponseProjection()
            .identer(
                new IdentInformasjonResponseProjection()
                    .ident());

        try {
            // med en gruppe og uten historikk forventer vi kun en gyldig ident for angitt
            var identer = pdlKlient.hentIdenter(request, projeksjon).getIdenter();
            if (identer.size() > 1) {
                throw new IllegalStateException("Fant fler enn 1 ident for angitt gruppe (regner ikke med historikk): " + identGruppe);
            } else if (identer.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(identer.get(0));
        } catch (VLException e) {
            if (PdlKlient.PDL_KLIENT_NOT_FOUND_KODE.equals(e.getKode())) {
                log.info("Ident av type {} ikke funnet for ident {}", identGruppe, ident);
                return Optional.empty();
            }
            throw e;
        }
    }
}
