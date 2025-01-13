package no.nav.ung.sak.tilgangskontroll.tilganger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.*;
import no.nav.k9.felles.util.LRUCache;
import no.nav.ung.sak.tilgangskontroll.rest.SkjermetPersonRestKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PersonDiskresjonskodeTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(PersonDiskresjonskodeTjeneste.class);

    private PdlKlient pdlKlient;
    private SkjermetPersonRestKlient skjermetPersonRestKlient;

    private Duration cacheExpiration = Duration.ofHours(1);
    private int cacheSize = 10;

    private LRUCache<AktørId, Set<Diskresjonskode>> cacheAktørIdTilDiskresjonskode = new LRUCache<>(cacheSize, cacheExpiration.toMillis());
    private LRUCache<PersonIdent, Set<Diskresjonskode>> cachePersonIdentTilDiskresjonskode = new LRUCache<>(cacheSize, cacheExpiration.toMillis());

    PersonDiskresjonskodeTjeneste() {
    }

    @Inject
    public PersonDiskresjonskodeTjeneste(PdlKlient pdlKlient, SkjermetPersonRestKlient skjermetPersonRestKlient) {
        this.pdlKlient = pdlKlient;
        this.skjermetPersonRestKlient = skjermetPersonRestKlient;
    }

    public Set<Diskresjonskode> hentDiskresjonskoder(AktørId aktørId) {
        Set<Diskresjonskode> cachetVerdi = cacheAktørIdTilDiskresjonskode.get(aktørId);
        if (cachetVerdi != null) {
            return cachetVerdi;
        }
        Set<Diskresjonskode> diskresjonskode = EnumSet.noneOf(Diskresjonskode.class);
        diskresjonskode.addAll(hentAdressebeskyttelseFraPdl(aktørId));
        if (erPersonSkjermet(aktørId)){
            diskresjonskode.add(Diskresjonskode.SKJERMET);
        }
        cacheAktørIdTilDiskresjonskode.put(aktørId, diskresjonskode);
        return diskresjonskode;
    }

    public Set<Diskresjonskode> hentDiskresjonskoder(PersonIdent personIdent) {
        Set<Diskresjonskode> cachetVerdi = cachePersonIdentTilDiskresjonskode.get(personIdent);
        if (cachetVerdi != null) {
            return cachetVerdi;
        }
        Set<Diskresjonskode> diskresjonskode = EnumSet.noneOf(Diskresjonskode.class);
        diskresjonskode.addAll(hentAdressebeskyttelseFraPdl(personIdent));
        if (erPersonSkjermet(personIdent)){
            diskresjonskode.add(Diskresjonskode.SKJERMET);
        }
        cachePersonIdentTilDiskresjonskode.put(personIdent, diskresjonskode);
        return diskresjonskode;
    }

    public Boolean erPersonSkjermet(AktørId aktørId) {
        String personIdent = pdlKlient.hentPersonIdentForAktørId(aktørId.getId()).orElseThrow();
        return erPersonSkjermet(new PersonIdent(personIdent));
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

    private Set<Diskresjonskode> hentAdressebeskyttelseFraPdl(AktørId aktørId) {
        //TODO kan bruke eget endepunkt hos PDL for å gjøre dette kjappere, men foreløpig ikke behov pga lavt volum
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());
        var person = pdlKlient.hentPerson(query, projection);
        return diskresjonskodeFor(person.getAdressebeskyttelse());
    }

    private Set<Diskresjonskode> hentAdressebeskyttelseFraPdl(PersonIdent personIdent) {
        //TODO kan bruke eget endepunkt hos PDL for å gjøre dette kjappere, men foreløpig ikke behov pga lavt volum
        var query = new HentPersonQueryRequest();
        query.setIdent(personIdent.getIdent());
        var projection = new PersonResponseProjection()
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());
        var person = pdlKlient.hentPerson(query, projection);
        return diskresjonskodeFor(person.getAdressebeskyttelse());
    }

    static Set<Diskresjonskode> diskresjonskodeFor(List<no.nav.k9.felles.integrasjon.pdl.Adressebeskyttelse> adressebeskyttelse) {
        return adressebeskyttelse.stream()
            .map(no.nav.k9.felles.integrasjon.pdl.Adressebeskyttelse::getGradering)
            .map(PersonDiskresjonskodeTjeneste::tilDiskresjonskode)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private static Diskresjonskode tilDiskresjonskode(AdressebeskyttelseGradering adressebeskyttelseGradering) {
        return switch (adressebeskyttelseGradering) {
            case STRENGT_FORTROLIG_UTLAND, STRENGT_FORTROLIG -> Diskresjonskode.KODE6;
            case FORTROLIG -> Diskresjonskode.KODE7;
            default -> null;
        };
    }
}
