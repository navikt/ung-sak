package no.nav.k9.sak.produksjonsstyring.behandlingenhet;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.person.Diskresjonskode;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRequest;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingResponse;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRestKlient;

@ApplicationScoped
public class EnhetsTjeneste {

    static class EnhetsTjenesteData {
        OrganisasjonsEnhet enhetKode6;
        OrganisasjonsEnhet enhetKlage;
        List<OrganisasjonsEnhet> alleBehandlendeEnheter;
        LocalDate sisteInnhenting = LocalDate.MIN;
        
        Optional<OrganisasjonsEnhet> finnOrganisasjonsEnhet(String enhetId) {
            return alleBehandlendeEnheter.stream().filter(e -> enhetId.equals(e.getEnhetId())).findFirst();
        }
    }
    
    private static final String DISKRESJON_K6 = Diskresjonskode.KODE6.getOffisiellKode(); // Kodeverk Diskresjonskoder
    private static final String NK_ENHET_ID = "4292";
    private static final OrganisasjonsEnhet KLAGE_ENHET =  new OrganisasjonsEnhet(NK_ENHET_ID, "NAV Klageinstans Midt-Norge");

    private static final Map<FagsakYtelseType, String> TEMAER = Map.of(
        FagsakYtelseType.OMSORGSPENGER, "OMS", // Alt på gammelt Infotrygd Tema 'OMS'
        FagsakYtelseType.PLEIEPENGER_SYKT_BARN, "OMS", // Alt på gammelt Infotrygd Tema 'OMS'
        FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, "OMS", // Alt på gammelt Infotrygd Tema 'OMS'
        FagsakYtelseType.OPPLÆRINGSPENGER, "OMS", // Alt på gammelt Infotrygd Tema 'OMS'
        FagsakYtelseType.FRISINN, "FRI" // Ny korona ytelse
    );

    private TpsTjeneste tpsTjeneste;
    private ArbeidsfordelingRestKlient arbeidsfordelingTjeneste;

    private Map<FagsakYtelseType, EnhetsTjenesteData> cache = Map.of(
        FagsakYtelseType.OMSORGSPENGER, new EnhetsTjenesteData(),
        FagsakYtelseType.PLEIEPENGER_SYKT_BARN, new EnhetsTjenesteData(),
        FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE, new EnhetsTjenesteData(),
        FagsakYtelseType.OPPLÆRINGSPENGER, new EnhetsTjenesteData(),
        FagsakYtelseType.FRISINN, new EnhetsTjenesteData());

    public EnhetsTjeneste() {
        // For CDI proxy
    }

    @Inject
    public EnhetsTjeneste(TpsTjeneste tpsTjeneste, ArbeidsfordelingRestKlient arbeidsfordelingTjeneste) {
        this.tpsTjeneste = tpsTjeneste;
        this.arbeidsfordelingTjeneste = arbeidsfordelingTjeneste;
    }

    List<OrganisasjonsEnhet> hentEnhetListe(FagsakYtelseType ytelseType) {
        var cacheEntry = oppdaterEnhetCache(ytelseType);
        return cacheEntry.alleBehandlendeEnheter;
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkOppgittePersoner(FagsakYtelseType ytelseType, String enhetId, AktørId hovedAktør, Collection<AktørId> alleAktører) {
        var cacheEntry = oppdaterEnhetCache(ytelseType);
        if (cacheEntry.enhetKode6.getEnhetId().equals(enhetId) || cacheEntry.enhetKlage.getEnhetId().equals(enhetId)) {
            return Optional.empty();
        }

        if (harNoenDiskresjonskode6(alleAktører)) {
            return Optional.of(cacheEntry.enhetKode6);
        }
        if (finnOrganisasjonsEnhet(ytelseType, enhetId).isEmpty()) {
            return Optional.of(hentEnhetSjekkKunAktør(hovedAktør, ytelseType));
        }
        return Optional.empty();
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkRegistrerteRelasjoner(FagsakYtelseType ytelseType, 
                                                                         String enhetId, 
                                                                         AktørId hovedAktør, 
                                                                         Collection<AktørId> alleAktører) {
        var cacheEntry = oppdaterEnhetCache(ytelseType);
        if (cacheEntry.enhetKode6.getEnhetId().equals(enhetId) || NK_ENHET_ID.equals(enhetId)) {
            return Optional.empty();
        }
        if (harNoenDiskresjonskode6(alleAktører)) {
            return Optional.of(cacheEntry.enhetKode6);
        }
        if (cacheEntry.finnOrganisasjonsEnhet(enhetId).isEmpty()) {
            return Optional.of(hentEnhetSjekkKunAktør(hovedAktør, ytelseType));
        }
        return Optional.empty();
    }
    
    OrganisasjonsEnhet hentEnhetSjekkKunAktør(AktørId aktørId, FagsakYtelseType ytelseType) {
        PersonIdent fnr = tpsTjeneste.hentFnrForAktør(aktørId);
        GeografiskTilknytning geografiskTilknytning = tpsTjeneste.hentGeografiskTilknytning(fnr);
        return hentEnheterFor(geografiskTilknytning.getTilknytning(), geografiskTilknytning.getDiskresjonskode(), ytelseType).get(0);
    }

    OrganisasjonsEnhet enhetsPresedens(FagsakYtelseType ytelseType, OrganisasjonsEnhet enhetSak1, OrganisasjonsEnhet enhetSak2) {
        var cacheEntry = oppdaterEnhetCache(ytelseType);
        if (cacheEntry.enhetKode6.getEnhetId().equals(enhetSak1.getEnhetId()) || cacheEntry.enhetKode6.getEnhetId().equals(enhetSak2.getEnhetId())) {
            return cacheEntry.enhetKode6;
        }
        return enhetSak1;
    }
    
    private boolean harNoenDiskresjonskode6(Collection<AktørId> aktører) {
        return aktører.stream()
            .map(tpsTjeneste::hentFnrForAktør)
            .map(tpsTjeneste::hentGeografiskTilknytning)
            .map(GeografiskTilknytning::getDiskresjonskode)
            .filter(Objects::nonNull)
            .anyMatch(DISKRESJON_K6::equalsIgnoreCase);
    }
    
    private EnhetsTjenesteData oppdaterEnhetCache(FagsakYtelseType ytelseType) {
        var cacheEntry = cache.get(ytelseType);
        if (cacheEntry.sisteInnhenting.isBefore(LocalDate.now())) {
            synchronized (cacheEntry) {
                if (cacheEntry.sisteInnhenting.isBefore(LocalDate.now())) {
                    cacheEntry.enhetKode6 = hentEnheterFor(null, Diskresjonskode.KODE6.getKode(), ytelseType).get(0);
                    cacheEntry.enhetKlage = KLAGE_ENHET;
                    cacheEntry.alleBehandlendeEnheter = hentEnheterFor(null, null, ytelseType);
                    cacheEntry.sisteInnhenting = LocalDate.now();
                }
            }
        }
        return cacheEntry;
    }

    OrganisasjonsEnhet getEnhetKlage() {
        return KLAGE_ENHET;
    }
    
    Optional<OrganisasjonsEnhet> finnOrganisasjonsEnhet(FagsakYtelseType ytelseType, String enhetId) {
        var cacheEntry = oppdaterEnhetCache(ytelseType);
        return cacheEntry.alleBehandlendeEnheter.stream().filter(e -> enhetId.equals(e.getEnhetId())).findFirst();
    }

    private List<OrganisasjonsEnhet> hentEnheterFor(String geografi, String diskresjon, FagsakYtelseType ytelseType) {
        List<ArbeidsfordelingResponse> restenhet;
        var request = ArbeidsfordelingRequest.ny()
            .medTema(TEMAER.get(ytelseType))
            .medTemagruppe("FMLI") // dekker OMS, PSB, FRISINN (fra Temagruppe offisielt kodeverk)
            .medOppgavetype("BEH_SAK_VL") // fra Oppgavetype offisielt kodeverk)
            .medBehandlingstype(BehandlingType.FØRSTEGANGSSØKNAD.getOffisiellKode()) // fra BehandlingType offisielt kodeverk
            .medDiskresjonskode(diskresjon)
            .medGeografiskOmraade(geografi)
            .build();
        if (geografi == null && diskresjon == null) {
            restenhet = arbeidsfordelingTjeneste.hentAlleAktiveEnheter(request);
        } else {
            restenhet = arbeidsfordelingTjeneste.finnEnhet(request);
        }
        return restenhet.stream()
            .map(r -> new OrganisasjonsEnhet(r.getEnhetNr(), r.getEnhetNavn()))
            .collect(Collectors.toList());
    }
}
