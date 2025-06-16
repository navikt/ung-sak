package no.nav.ung.sak.produksjonsstyring.behandlingenhet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRequest;
import no.nav.k9.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingResponse;
import no.nav.k9.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRestKlient;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.person.Diskresjonskode;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@ApplicationScoped
public class EnhetsTjeneste {

    static class EnhetsTjenesteData {
        OrganisasjonsEnhet enhetKode6;
        OrganisasjonsEnhet enhetKlage;
        List<OrganisasjonsEnhet> alleBehandlendeEnheter;
        LocalDate sisteInnhenting = LocalDate.MIN;
    }

    private static final String NK_ENHET_ID = "4292";
    private static final OrganisasjonsEnhet KLAGE_ENHET = new OrganisasjonsEnhet(NK_ENHET_ID, "NAV Klageinstans Midt-Norge");

    private TpsTjeneste tpsTjeneste;
    private ArbeidsfordelingRestKlient arbeidsfordelingTjeneste;

    private final Map<FagsakYtelseType, EnhetsTjenesteData> cache =
        Arrays.stream(FagsakYtelseType.values())
            .collect(Collectors.toMap(v -> v, v -> new EnhetsTjenesteData()));

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

    public List<OrganisasjonsEnhet> hentFordelingEnhetId(OmrådeTema tema, BehandlingTema behandlingsTema, GeografiskTilknytning geo) {
        var request = ArbeidsfordelingRequest.ny()
            .medTema(tema.getOffisiellKode())
            .medBehandlingstema(behandlingsTema == null ? null : behandlingsTema.getOffisiellKode())
            .medDiskresjonskode(geo.getDiskresjonskode().getKode())
            .medGeografiskOmraade(geo.getTilknytning())
            .build();

        return arbeidsfordelingTjeneste
            .finnEnhet(request)
            .stream()
            .filter(response -> "AKTIV".equalsIgnoreCase(response.getStatus()))
            .map(r -> new OrganisasjonsEnhet(r.getEnhetNr(), r.getEnhetNavn()))
            .collect(Collectors.toList());
    }

    OrganisasjonsEnhet hentEnhetSjekkKunAktør(AktørId aktørId, FagsakYtelseType ytelseType) {
        PersonIdent fnr = tpsTjeneste.hentFnrForAktør(aktørId);
        GeografiskTilknytning geografiskTilknytning = tpsTjeneste.hentGeografiskTilknytning(fnr);
        return hentEnheterFor(geografiskTilknytning.getTilknytning(), ofNullable(geografiskTilknytning.getDiskresjonskode()).map(Diskresjonskode::getKode).orElse(null), ytelseType).get(0);
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
            .anyMatch(Diskresjonskode.KODE6::equals);
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
            .medTema(FagsakYtelseType.UNGDOMSYTELSE.getOppgavetema())
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
