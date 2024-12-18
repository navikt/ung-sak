package no.nav.ung.sak.domene.behandling.steg.simulering;

import static java.util.Collections.singletonList;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.SIMULER_OPPDRAG;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.exception.IntegrasjonException;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjeneste;
import no.nav.ung.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;

@BehandlingStegRef(value = SIMULER_OPPDRAG)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class SimulerOppdragSteg implements BehandlingSteg {

    private static final Logger logger = LoggerFactory.getLogger(SimulerOppdragSteg.class);

    private static final int ÅPNINGSTID = 7;
    private static final int STENGETID = 21;

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private K9TilbakeRestKlient k9TilbakeRestKlient;

    SimulerOppdragSteg() {
        // for CDI proxy
    }

    @Inject
    public SimulerOppdragSteg(BehandlingRepositoryProvider repositoryProvider,
                              BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                              SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste,
                              TilbakekrevingRepository tilbakekrevingRepository,
                              K9TilbakeRestKlient k9TilbakeRestKlient) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.simuleringIntegrasjonTjeneste = simuleringIntegrasjonTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.k9TilbakeRestKlient = k9TilbakeRestKlient;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        try {
            startSimulering(behandling);
            return utledAksjonspunkt(behandling);
        } catch (IntegrasjonException e) {
            LocalDateTime nesteKjøringEtter = utledNesteKjøring();
            opprettFortsettBehandlingTask(behandling, nesteKjøringEtter);
            logger.info("Det oppstod IntegrasjonException mot oppdragsystemet. Setter på vent til " + nesteKjøringEtter + ". Dette er normalt når oppdragssystemet har planlagt nedetid, for eksempel kveld og helg.", e);
            return BehandleStegResultat.settPåVent();
        }
    }

    @Override
    public BehandleStegResultat gjenopptaSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        startSimulering(behandling);
        return utledAksjonspunkt(behandling);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!SIMULER_OPPDRAG.equals(tilSteg)) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            simuleringIntegrasjonTjeneste.kansellerSimulering(behandling);
            tilbakekrevingRepository.deaktiverEksisterendeTilbakekrevingValg(behandling);
            tilbakekrevingRepository.deaktiverEksisterendeTilbakekrevingInntrekk(behandling);
        }
    }

    private void startSimulering(Behandling behandling) {
        simuleringIntegrasjonTjeneste.startSimulering(behandling);
    }

    private void opprettFortsettBehandlingTask(Behandling behandling, LocalDateTime nesteKjøringEtter) {
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandlingGjenopptaStegNesteKjøring(behandling,
            SIMULER_OPPDRAG, nesteKjøringEtter);
    }

    private BehandleStegResultat utledAksjonspunkt(Behandling behandling) {
        if (harÅpenTilbakekreving(behandling)) {
            lagreTilbakekrevingValg(behandling, TilbakekrevingValg.medOppdaterTilbakekrevingsbehandling());
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        Optional<SimuleringResultatDto> simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling);
        if (simuleringResultatDto.isPresent()) {
            SimuleringResultatDto resultatDto = simuleringResultatDto.get();
            tilbakekrevingRepository.lagre(behandling, resultatDto.isSlåttAvInntrekk());

            if (resultatDto.harFeilutbetaling()) {
                tilbakekrevingRepository.reaktiverForrigeTilbakekrevingValg(behandling);
                return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(AksjonspunktDefinisjon.VURDER_FEILUTBETALING));
            }
            if (resultatDto.harInntrekkmulighet()) {
                lagreTilbakekrevingValg(behandling, TilbakekrevingValg.medAutomatiskInntrekk());
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private LocalDateTime utledNesteKjøring() {
        LocalDateTime currentTime = LocalDateTime.now();
        if (DayOfWeek.SATURDAY.equals(currentTime.getDayOfWeek()) || DayOfWeek.SUNDAY.equals(currentTime.getDayOfWeek())) {
            return kommendeMandag(currentTime);
        } else if (DayOfWeek.FRIDAY.equals(currentTime.getDayOfWeek()) && currentTime.getHour() > STENGETID) {
            return kommendeMandag(currentTime);
        } else if (currentTime.getHour() < ÅPNINGSTID) {
            return currentTime.withHour(ÅPNINGSTID).withMinute(15);
        } else if (currentTime.getHour() > STENGETID) {
            return currentTime.plusDays(1).withHour(ÅPNINGSTID).withMinute(15);
        }
        return null; // bruker default innenfor åpningstid
    }

    private LocalDateTime kommendeMandag(LocalDateTime currentTime) {
        return currentTime.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).withHour(ÅPNINGSTID).withMinute(15);
    }

    private boolean harÅpenTilbakekreving(Behandling behandling) {
        return k9TilbakeRestKlient.harÅpenTilbakekrevingsbehandling(behandling.getFagsak().getSaksnummer());
    }

    private void lagreTilbakekrevingValg(Behandling behandling, TilbakekrevingValg tilbakekrevingValg) {
        tilbakekrevingRepository.lagre(behandling, tilbakekrevingValg);
    }
}
