package no.nav.k9.sak.domene.person.pdl;

import static java.util.Optional.ofNullable;
import static org.jboss.weld.util.collections.ImmutableList.of;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.pdl.HentIdenterQueryRequest;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.IdentInformasjonResponseProjection;
import no.nav.pdl.IdentlisteResponseProjection;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumer;
import no.nav.vedtak.felles.integrasjon.aktør.klient.DetFinnesFlereAktørerMedSammePersonIdentException;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;
import no.nav.vedtak.felles.integrasjon.pdl.Tema;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class AktørTjeneste {

    private static final Logger log = LoggerFactory.getLogger(AktørTjeneste.class);

    private static final int DEFAULT_CACHE_SIZE = 1000;
    private static final long DEFAULT_CACHE_TIMEOUT = TimeUnit.MILLISECONDS.convert(8, TimeUnit.HOURS);

    private LRUCache<AktørId, PersonIdent> cacheAktørIdTilIdent;
    private LRUCache<PersonIdent, AktørId> cacheIdentTilAktørId;

    private AktørConsumer aktørConsumer;
    private PdlKlient pdlKlient;

    @SuppressWarnings("unused")
    AktørTjeneste() {
        // CDI
    }

    @Inject
    public AktørTjeneste(PdlKlient pdlKlient,
                         AktørConsumer aktørConsumer) {
        this.aktørConsumer = aktørConsumer;
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
        try {
            Optional<AktørId> aktørIdFraTps = aktørConsumer.hentAktørIdForPersonIdent(personIdent.getIdent()).map(AktørId::new);
            aktørIdFraTps.ifPresent(aktørId -> {
                    sjekkAktørIdFraPDL(personIdent.getIdent(), aktørId.getId());
                    // Kan ikke legge til i cache aktørId -> ident ettersom ident kan være ikke-current
                    cacheIdentTilAktørId.put(personIdent, aktørId);
                }
            );
            return aktørIdFraTps;
        } catch (DetFinnesFlereAktørerMedSammePersonIdentException e) { // NOSONAR
            // Her sorterer vi ut dødfødte barn
            return Optional.empty();
        }
    }

    public Optional<PersonIdent> hentPersonIdentForAktørId(AktørId aktørId) {
        var personIdentFraCache = ofNullable(cacheAktørIdTilIdent.get(aktørId));
        if (personIdentFraCache.isPresent()) {
            return personIdentFraCache;
        }

        Optional<PersonIdent> personIdent = aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(PersonIdent::new);
        personIdent.ifPresent(i -> {
                sjekkPersonIdentFraPDL(aktørId.getId(), i.getIdent());
                cacheAktørIdTilIdent.put(aktørId, i);
                cacheIdentTilAktørId.put(i, aktørId); // OK her, men ikke over ettersom dette er gjeldende mapping
            }
        );

        return personIdent;
    }

    private void sjekkAktørIdFraPDL(String personIdent, String aktørFraConsumer) {
        try {
            var aktørIder = identerFor(personIdent, IdentGruppe.AKTORID);
            int antall = aktørIder.size();
            var aktørId = aktørIder.stream().findFirst().map(IdentInformasjon::getIdent).orElse(null);

            if (antall == 1 && Objects.equals(aktørFraConsumer, aktørId)) {
                log.info("PDL personIdent->aktørid: like aktørid");
            } else if (antall != 1 && Objects.equals(aktørFraConsumer, aktørId)) {
                log.info("PDL personIdent->aktørid: ulikt antall aktørid {}", antall);
            } else {
                log.info("PDL personIdent->aktørid: ulike aktørid TPS {} og PDL {} antall {}", aktørFraConsumer, aktørId, antall);
            }
        } catch (Exception e) {
            log.info("PDL personIdent->aktørid error", e);
        }
    }

    private void sjekkPersonIdentFraPDL(String aktørId, String personIdentFraTps) {
        try {
            var personIdenter = identerFor(aktørId, IdentGruppe.FOLKEREGISTERIDENT);
            int antall = personIdenter.size();
            var personIdent = personIdenter.stream().findFirst().map(IdentInformasjon::getIdent).orElse(null);

            if (antall == 1 && Objects.equals(personIdentFraTps, personIdent)) {
                log.info("PDL aktørid->personIdent: like identer");
            } else if (antall != 1 && Objects.equals(personIdentFraTps, personIdent)) {
                log.info("PDL aktørid->personIdent: ulikt antall identer {}", antall);
            } else {
                log.info("PDL aktørid->personIdent: ulike identer TPS {} og PDL {} antall {}", personIdentFraTps, personIdent, antall);
            }
        } catch (Exception e) {
            log.info("PDL aktørid->fnr error", e);
        }
    }

    private List<IdentInformasjon> identerFor(String ident, IdentGruppe identGruppe) {
        HentIdenterQueryRequest request = new HentIdenterQueryRequest();
        request.setIdent(ident);
        request.setGrupper(of(identGruppe));
        request.setHistorikk(Boolean.FALSE);

        IdentlisteResponseProjection projeksjon = new IdentlisteResponseProjection()
            .identer(
                new IdentInformasjonResponseProjection()
                    .ident()
                    .gruppe()
            );

        return pdlKlient.hentIdenter(request, projeksjon, Tema.FOR).getIdenter();
    }


}
