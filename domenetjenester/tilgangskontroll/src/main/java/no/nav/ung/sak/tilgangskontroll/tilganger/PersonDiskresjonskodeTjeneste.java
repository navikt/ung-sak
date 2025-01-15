package no.nav.ung.sak.tilgangskontroll.tilganger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.util.LRUCache;
import no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.PersonPipRestKlient;
import no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.SystemUserPdlKlient;
import no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto.AdressebeskyttelseGradering;
import no.nav.ung.sak.tilgangskontroll.integrasjon.skjermetperson.SkjermetPersonRestKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class PersonDiskresjonskodeTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(PersonDiskresjonskodeTjeneste.class);

    private PersonPipRestKlient pdlPipKlient;
    private SystemUserPdlKlient pdlKlient;
    private SkjermetPersonRestKlient skjermetPersonRestKlient;

    private final LRUCache<PersonIdent, Set<Diskresjonskode>> personIdentTilDiskresjonskodeCache = new LRUCache<>(10, Duration.ofHours(1).toMillis());
    private final LRUCache<AktørId, PersonIdent> aktørIdTilPersonIdentCache = new LRUCache<>(10, Duration.ofHours(1).toMillis());


    PersonDiskresjonskodeTjeneste() {
    }

    @Inject
    public PersonDiskresjonskodeTjeneste(PersonPipRestKlient pdlKlient, SystemUserPdlKlient pdlKlient1, SkjermetPersonRestKlient skjermetPersonRestKlient) {
        this.pdlPipKlient = pdlKlient;
        this.pdlKlient = pdlKlient1;
        this.skjermetPersonRestKlient = skjermetPersonRestKlient;
    }

    public Set<Diskresjonskode> hentDiskresjonskoder(Collection<AktørId> aktørIder, Collection<PersonIdent> personIdenter) {
        Set<PersonIdent> allePersonIdenter = new LinkedHashSet<>();
        allePersonIdenter.addAll(personIdenter);
        allePersonIdenter.addAll(aktørIder.stream().map(this::hentPersonIdent).toList());

        return allePersonIdenter.stream()
            .flatMap(personIdent -> hentDiskresjonskoder(personIdent).stream())
            .collect(Collectors.toSet());
    }

    private PersonIdent hentPersonIdent(AktørId aktørId) {
        return CacheOppfriskingHåndterer.hentOmIkkeICache(aktørId, aktørIdTilPersonIdentCache, this::internHentPersonIdent);
    }

    private PersonIdent internHentPersonIdent(AktørId aktørId) {
        return PersonIdent.fra(pdlKlient.hentPersonIdentForAktørId(aktørId.getAktørId()).orElseThrow());
    }

    public Set<Diskresjonskode> hentDiskresjonskoder(PersonIdent personIdent) {
        return CacheOppfriskingHåndterer.hentOmIkkeICache(personIdent, personIdentTilDiskresjonskodeCache, this::internHentDiskresjonskoder);
    }

    public Set<Diskresjonskode> internHentDiskresjonskoder(PersonIdent personIdent) {
        Set<Diskresjonskode> diskresjonskode = EnumSet.noneOf(Diskresjonskode.class);
        diskresjonskode.addAll(hentAdressebeskyttelseFraPdl(personIdent));
        if (erPersonSkjermet(personIdent)) {
            diskresjonskode.add(Diskresjonskode.SKJERMET);
        }
        return diskresjonskode;
    }

    private Boolean erPersonSkjermet(PersonIdent personIdent) {
        Boolean skjermet = skjermetPersonRestKlient.personErSkjermet(personIdent);
        if (skjermet == null) {
            logger.warn("Fikk tomt resultat for skjerming. Behandler som at person er skjermet");
            return true;
        } else {
            return skjermet;
        }
    }

    private Set<Diskresjonskode> hentAdressebeskyttelseFraPdl(PersonIdent personIdent) {
        var adressebeskyttelseKoder = pdlPipKlient.hentAdressebeskyttelse(personIdent);
        return diskresjonskodeFor(adressebeskyttelseKoder);
    }

    static Set<Diskresjonskode> diskresjonskodeFor(Set<AdressebeskyttelseGradering> adressebeskyttelser) {
        return adressebeskyttelser.stream()
            .map(PersonDiskresjonskodeTjeneste::tilDiskresjonskode)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private static Diskresjonskode tilDiskresjonskode(AdressebeskyttelseGradering adressebeskyttelseGradering) {
        return switch (adressebeskyttelseGradering) {
            case STRENGT_FORTROLIG_UTLAND, STRENGT_FORTROLIG -> Diskresjonskode.KODE6;
            case FORTROLIG -> Diskresjonskode.KODE7;
            case UGRADERT -> null;
        };
    }
}
