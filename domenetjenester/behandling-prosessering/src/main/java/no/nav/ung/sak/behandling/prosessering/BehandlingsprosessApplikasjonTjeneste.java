package no.nav.ung.sak.behandling.prosessering;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkBegrunnelseType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.ung.sak.behandling.prosessering.task.FortsettBehandlingTask;
import no.nav.ung.sak.behandling.prosessering.task.ÅpneBehandlingForEndringerTask;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.ung.sak.typer.AktørId;

@Dependent
public class BehandlingsprosessApplikasjonTjeneste {

    private BehandlingRepository behandlingRepository;
    private HistorikkRepository historikkRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    BehandlingsprosessApplikasjonTjeneste() {
        // for CDI proxy
    }

    // test only
    public BehandlingsprosessApplikasjonTjeneste(ProsesseringAsynkTjeneste prosesseringAsynkTjeneste) {
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
    }

    @Inject
    public BehandlingsprosessApplikasjonTjeneste(
        BehandlingRepository behandlingRepository,
        ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
        BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
        HistorikkRepository historikkRepository, BehandlingskontrollTjeneste behandlingskontrollTjeneste) {

        Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.behandlingRepository = behandlingRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.historikkRepository = historikkRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    /**
     * Kjører prosess, (henter ikke inn registeropplysninger på nytt selv)
     *
     * @return Prosess Task gruppenavn som kan brukes til å sjekke fremdrift
     */
    public String asynkKjørProsess(Behandling behandling) {
        return prosesseringAsynkTjeneste.asynkProsesserBehandlingMergeGruppe(behandling);
    }

    /**
     * Kjører prosess, (henter ikke inn registeropplysninger på nytt selv)
     *
     * @return Prosess Task gruppenavn som kan brukes til å sjekke fremdrift
     */
    public String asynkRegisteroppdateringKjørProsess(Behandling behandling) {
        behandlingProsesseringTjeneste.tvingInnhentingRegisteropplysninger(behandling);
        return asynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling);
    }

    /**
     * Kjør behandlingsprosess asynkront videre for nyopprettet behandling.
     *
     * @return ProsessTask gruppe
     */
    public String asynkStartBehandlingsprosess(Behandling behandling) {
        return prosesseringAsynkTjeneste.asynkStartBehandlingProsess(behandling);
    }

    /**
     * Innhent registeropplysninger og kjør prosess asynkront.
     *
     * @return Prosess Task gruppenavn som kan brukes til å sjekke fremdrift
     */
    private String asynkInnhentingAvRegisteropplysningerOgKjørProsess(Behandling behandling) {
        ProsessTaskGruppe gruppe = behandlingProsesseringTjeneste.lagOppdaterFortsettTasksForPolling(behandling);
        String gruppeNavn = prosesseringAsynkTjeneste.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(behandling.getFagsakId(), String.valueOf(behandling.getId()),
            gruppe);
        return gruppeNavn;
    }

    /**
     * Innhent registeropplysninger og kjør prosess asynkront.
     *
     * @return Prosess Task gruppenavn som kan brukes til å sjekke fremdrift
     */
    public String asynkInnhentingAvRegisteropplysningerOgKjørProsess(Behandling behandling, boolean forceInnhent) {
        ProsessTaskGruppe gruppe = behandlingProsesseringTjeneste.lagOppdaterFortsettTasksForPolling(behandling, forceInnhent);
        String gruppeNavn = prosesseringAsynkTjeneste.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(behandling.getFagsakId(), String.valueOf(behandling.getId()),
            gruppe);
        return gruppeNavn;
    }

    /**
     * Gjenoppta behandling, start innhenting av registeropplysninger på nytt og kjør prosess hvis nødvendig.
     *
     * @return gruppenavn (prosesstask) hvis noe startet asynkront.
     */
    public Optional<String> gjenopptaBehandling(Behandling behandling) {
        opprettHistorikkinnslagForManueltGjenopptakelse(behandling, HistorikkinnslagType.BEH_MAN_GJEN);
        return Optional.of(asynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling));
    }

    public Behandling hentBehandling(Long behandlingsId) {
        return behandlingRepository.hentBehandling(behandlingsId);
    }

    public Behandling hentBehandling(UUID behandlingUuid) {
        return behandlingRepository.hentBehandling(behandlingUuid);
    }


    /**
     * Åpner behandlingen for endringer ved å reaktivere inaktive aksjonspunkter fra steget etter Innhent registeroppl.
     * Gjøres asynkront.
     *
     * @return ProsessTask gruppe
     */
    public String asynkTilbakestillOgÅpneBehandlingForEndringer(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var startSteg = behandlingskontrollTjeneste.nesteSteg(behandling, BehandlingStegType.INNHENT_REGISTEROPP)
            .orElseThrow(() -> new IllegalStateException("Behandling kan ikke åpnes for endringer fra et veldefinert steg"));

        return asynkTilbakestillOgÅpneBehandlingForEndringer(behandlingId, startSteg);
    }


    /**
     * Åpner behandlingen for endringer ved å reaktivere inaktive aksjonspunkter før startpunktet
     * og hopper til første startpunkt. Gjøres asynkront.
     *
     * @return ProsessTask gruppe
     */
    public String asynkTilbakestillOgÅpneBehandlingForEndringer(Long behandlingId, BehandlingStegType startSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        AktørId aktørId = behandling.getAktørId();
        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();

        ProsessTaskData åpneBehandlingForEndringerTask =  ProsessTaskData.forProsessTask(ÅpneBehandlingForEndringerTask.class);
        åpneBehandlingForEndringerTask.setProperty(ÅpneBehandlingForEndringerTask.START_STEG, startSteg.getKode());
        åpneBehandlingForEndringerTask.setBehandling(behandling.getFagsakId(), behandlingId, aktørId.getId());
        gruppe.addNesteSekvensiell(åpneBehandlingForEndringerTask);

        ProsessTaskData fortsettBehandlingTask =  ProsessTaskData.forProsessTask(FortsettBehandlingTask.class);
        fortsettBehandlingTask.setBehandling(behandling.getFagsakId(), behandlingId, aktørId.getId());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);

        opprettHistorikkinnslagForBehandlingStartetPåNytt(behandling);
        gruppe.setCallIdFraEksisterende();
        return prosesseringAsynkTjeneste.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(behandling.getFagsakId(), String.valueOf(behandlingId), gruppe);
    }

    /**
     * På grunn av (nyinnført) async-prosessering videre nedover mister vi informasjon her om at det i dette tilfellet er saksbehandler som
     * ber om gjenopptakelse av behandlingen. Det kommer et historikkinnslag om dette (se
     * {@link AksjonspunktStatusEvent})
     * som eies av systembruker. Derfor velger vi her å legge på et innslag til med saksbehandler som eier slik at historikken blir korrekt.
     */
    private void opprettHistorikkinnslagForManueltGjenopptakelse(Behandling behandling,
                                                                 HistorikkinnslagType historikkinnslagType) {
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder();
        builder.medHendelse(historikkinnslagType);

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setType(historikkinnslagType);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    private void opprettHistorikkinnslagForBehandlingStartetPåNytt(Behandling behandling) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.BEH_STARTET_PÅ_NYTT);
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BEH_STARTET_PÅ_NYTT)
            .medBegrunnelse(HistorikkBegrunnelseType.BEH_STARTET_PA_NYTT);
        historikkInnslagTekstBuilder.build(historikkinnslag);
        historikkinnslag.setBehandling(behandling);
        historikkRepository.lagre(historikkinnslag);
    }
}