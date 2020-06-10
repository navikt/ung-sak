package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.vedtak.feil.LogLevel.ERROR;
import static no.nav.vedtak.feil.LogLevel.WARN;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.behandling.SjekkProsessering;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.konfig.KonfigVerdi;

@Dependent
public class BehandlingsutredningApplikasjonTjeneste {

    private Period defaultVenteFrist;
    private BehandlingRepository behandlingRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private SjekkProsessering sjekkProsessering;
    BehandlingsutredningApplikasjonTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingsutredningApplikasjonTjeneste(@KonfigVerdi(value = "behandling.default.ventefrist.periode", defaultVerdi = "P4W") Period defaultVenteFrist,
                                                   BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                   OppgaveTjeneste oppgaveTjeneste,
                                                   BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                                   SjekkProsessering sjekkProsessering,
                                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        Objects.requireNonNull(behandlingRepositoryProvider, "behandlingRepositoryProvider");
        this.defaultVenteFrist = defaultVenteFrist;
        this.sjekkProsessering = sjekkProsessering;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    /**
     * Hent behandlinger for angitt saksnummer (offisielt GSAK saksnummer)
     */
    public List<Behandling> hentBehandlingerForSaksnummer(Saksnummer saksnummer) {
        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer);
        return behandlinger;
    }

    public void settBehandlingPaVent(Long behandlingsId, LocalDate frist, Venteårsak venteårsak, String venteårsakVariant) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;

        doSetBehandlingPåVent(behandlingsId, aksjonspunktDefinisjon, frist, venteårsak, venteårsakVariant);
    }

    private void doSetBehandlingPåVent(Long behandlingsId, 
                                       AksjonspunktDefinisjon apDef, 
                                       LocalDate frist,
                                       Venteårsak venteårsak, 
                                       String venteårsakVariant) {

        LocalDateTime fristTid = bestemFristForBehandlingVent(frist);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingsId);
        oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling);
        BehandlingStegType behandlingStegFunnet = behandling.getAksjonspunktMedDefinisjonOptional(apDef)
            .map(Aksjonspunkt::getBehandlingStegFunnet)
            .orElse(null); // Dersom autopunkt ikke allerede er opprettet, så er det ikke tilknyttet steg
        behandlingskontrollTjeneste.settBehandlingPåVent(behandling, apDef, behandlingStegFunnet, fristTid, venteårsak, venteårsakVariant);
    }

    public void endreBehandlingPaVent(Long behandlingsId, LocalDate frist, Venteårsak venteårsak, String venteårsakVariant) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingsId);
        if (!behandling.isBehandlingPåVent()) {
            throw BehandlingsutredningApplikasjonTjenesteFeil.FACTORY.kanIkkeEndreVentefristForBehandlingIkkePaVent(behandlingsId)
                .toException();
        }
        AksjonspunktDefinisjon aksjonspunktDefinisjon = behandling.getBehandlingPåVentAksjonspunktDefinisjon();
        doSetBehandlingPåVent(behandlingsId, aksjonspunktDefinisjon, frist, venteårsak, venteårsakVariant);
    }

    private LocalDateTime bestemFristForBehandlingVent(LocalDate frist) {
        return frist != null
            ? LocalDateTime.of(frist, LocalDateTime.now().toLocalTime())
            : LocalDateTime.now().plus(defaultVenteFrist);
    }

    public void byttBehandlendeEnhet(Long behandlingId, OrganisasjonsEnhet enhet, String begrunnelse, HistorikkAktør aktør) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, enhet, aktør, begrunnelse);
    }

    public void kanEndreBehandling(Long behandlingId, Long versjon) {
        Boolean uendret = behandlingRepository.erVersjonUendret(behandlingId, versjon);
        var beh = behandlingRepository.hentBehandling(behandlingId);
        if (!uendret) {
            throw BehandlingsutredningApplikasjonTjenesteFeil.FACTORY.endringerHarForekommetPåBehandlingen(behandlingId, versjon, beh.getVersjon()).toException();
        }
        validerIngenPågåendeProsess(beh);
    }
    
    private void validerIngenPågåendeProsess(Behandling behandling) {
        var res = sjekkProsessering.sjekkProsessTaskPågårForBehandling(behandling, null);
        if(res.isPresent()) {
            Fagsak fagsak = behandling.getFagsak();
            throw BehandlingsutredningApplikasjonTjenesteFeil.FACTORY.prosessUnderveisKanIkkeEndreTilKlart(fagsak.getYtelseType(), behandling.getId(), fagsak.getSaksnummer(), res.get()).toException();
        }
    }

    interface BehandlingsutredningApplikasjonTjenesteFeil extends DeklarerteFeil {
        BehandlingsutredningApplikasjonTjenesteFeil FACTORY = FeilFactory.create(BehandlingsutredningApplikasjonTjenesteFeil.class); // NOSONAR

        @FunksjonellFeil(feilkode = "K9-992332", feilmelding = "BehandlingId %s er ikke satt på vent, og ventefrist kan derfor ikke oppdateres", løsningsforslag = "Forsett saksbehandlingen", logLevel = ERROR)
        Feil kanIkkeEndreVentefristForBehandlingIkkePaVent(Long behandlingId);

        @TekniskFeil(feilkode = "K9-760741", feilmelding = "Behandling [ytelseType=%s, behandlingId=%s, saksnummer=%s] pågår eller feilet, kan ikke gjøre endringer inntil det er klart: %s", logLevel = WARN)
        Feil prosessUnderveisKanIkkeEndreTilKlart(FagsakYtelseType ytelseType, Long behandlingId, Saksnummer saksnummer, AsyncPollingStatus st);

        @FunksjonellFeil(feilkode = "K9-837578", feilmelding = "Behandlingen [%s] er endret av en annen saksbehandler, eller har blitt oppdatert med ny informasjon av systemet. Fikk versjon [%s], har versjon [%s]", løsningsforslag = "Last inn behandlingen på nytt.", logLevel = WARN, exceptionClass = BehandlingEndretKonfliktException.class)
        Feil endringerHarForekommetPåBehandlingen(Long behandlingId, Long versjonInn, Long versjonEksisterende);
    }
}
