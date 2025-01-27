package no.nav.ung.sak.tilgangskontroll.tilganger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.util.LRUCache;
import no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.PersonPipRestKlient;
import no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto.AdressebeskyttelseGradering;
import no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto.PipPersondataResponse;
import no.nav.ung.sak.tilgangskontroll.integrasjon.skjermetperson.SkjermetPersonRestKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class PersonDiskresjonskodeTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(PersonDiskresjonskodeTjeneste.class);

    private PersonPipRestKlient pdlPipKlient;
    private SkjermetPersonRestKlient skjermetPersonRestKlient;

    private final LRUCache<AktørId, PipPersondataResponse> aktørIdTilPersonCache = new LRUCache<>(10, Duration.ofHours(1).toMillis());
    private final LRUCache<PersonIdent, Boolean> personIdentTilSkjermetCache = new LRUCache<>(10, Duration.ofHours(1).toMillis());
    private final LRUCache<PersonIdent, Set<Diskresjonskode>> personIdentTilDiskresjonskodeCache = new LRUCache<>(10, Duration.ofHours(1).toMillis());


    PersonDiskresjonskodeTjeneste() {
    }

    @Inject
    public PersonDiskresjonskodeTjeneste(PersonPipRestKlient pdlKlient, SkjermetPersonRestKlient skjermetPersonRestKlient) {
        this.pdlPipKlient = pdlKlient;
        this.skjermetPersonRestKlient = skjermetPersonRestKlient;
    }

    public Set<Diskresjonskode> hentDiskresjonskoder(Collection<AktørId> aktørIder, Collection<PersonIdent> personIdenter) {
        Set<Diskresjonskode> alleDiskresjonskoder = new HashSet<>();

        //først ut diskresjonskoder (og personIdent) for personer som er representert med aktørId
        Map<AktørId, PipPersondataResponse> aktørIdTilPersoninformasjon = aktørIder.stream()
            .collect(Collectors.toMap(Function.identity(), aktørId -> CacheOppfriskingHåndterer.hentOmIkkeICache(aktørId, aktørIdTilPersonCache, aktøId -> pdlPipKlient.hentPersoninformasjon(aktørId))));
        Set<AdressebeskyttelseGradering> adressebeskyttelser = aktørIdTilPersoninformasjon.values().stream().flatMap(it -> it.getAdressebeskyttelseGradering().stream()).collect(Collectors.toSet());
        alleDiskresjonskoder.addAll(diskresjonskodeFor(adressebeskyttelser));
        List<PersonIdent> fnrFraAktørId = aktørIdTilPersoninformasjon.values().stream().map(v -> new PersonIdent(v.getAktivPersonIdent())).toList();
        if (fnrFraAktørId.stream().anyMatch(this::erPersonSkjermet)) {
            alleDiskresjonskoder.add(Diskresjonskode.SKJERMET);
        }

        //henter så diskresjonskoder for personer som er representert med personIdent
        for (PersonIdent personIdent : personIdenter) {
            if (fnrFraAktørId.contains(personIdent)) {
                //samme person er sendt inn både som aktørId og FNR, og er håndtert over
            } else {
                alleDiskresjonskoder.addAll(hentDiskresjonskoder(personIdent));
            }
        }

        return alleDiskresjonskoder;
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
        return CacheOppfriskingHåndterer.hentOmIkkeICache(personIdent, personIdentTilSkjermetCache, this::internErPersonSkjermet);
    }

    private Boolean internErPersonSkjermet(PersonIdent personIdent) {
        Boolean skjermet = skjermetPersonRestKlient.erPersonSkjermet(personIdent);
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
