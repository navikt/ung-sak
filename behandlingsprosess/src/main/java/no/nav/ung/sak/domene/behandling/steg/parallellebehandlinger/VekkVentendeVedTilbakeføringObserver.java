package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStegOvergangEvent.BehandlingStegTilbakeføringEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallellBehandling;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;

/**
 * Vekker behandlinger som venter i porten når behandlingen som holder porten, hopper tilbake til før porten.
 * <p>
 * Uten vekking ville de ventende behandlingene først gått videre ved fristen for autopunktet.
 */
@ApplicationScoped
public class VekkVentendeVedTilbakeføringObserver {

    private static final Logger log = LoggerFactory.getLogger(VekkVentendeVedTilbakeføringObserver.class);
    private static final BehandlingStegType PORTEN = BehandlingStegType.VURDER_PARALLELLE_BEHANDLINGER;

    private BehandlingRepository behandlingRepository;
    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private KontrollbehandlingBryter kontrollbehandlingBryter;

    VekkVentendeVedTilbakeføringObserver() {
        // for CDI proxy
    }

    @Inject
    public VekkVentendeVedTilbakeføringObserver(BehandlingRepository behandlingRepository,
                                                ParallelleBehandlingerRepository parallelleBehandlingerRepository,
                                                BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                                BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                                KontrollbehandlingBryter kontrollbehandlingBryter) {
        this.behandlingRepository = behandlingRepository;
        this.parallelleBehandlingerRepository = parallelleBehandlingerRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.kontrollbehandlingBryter = kontrollbehandlingBryter;
    }

    public void vedTilbakeføring(@Observes BehandlingStegTilbakeføringEvent event) {
        BehandlingStegType fraSteg = event.getFraStegType();
        BehandlingStegType tilSteg = event.getTilStegType();
        if (fraSteg == null || tilSteg == null || fraSteg == PORTEN) {
            // Fra porten: behandlingen sto i porten og holdt den ikke. Gjelder også ny kjøring av porten etter vent.
            return;
        }
        Behandling behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        if (!behandling.erYtelseBehandling() || !kontrollbehandlingBryter.erPå(behandling.getFagsakYtelseType())) {
            return;
        }
        if (!slipperPorten(behandling, fraSteg, tilSteg)) {
            return;
        }

        for (ParallellBehandling annen : parallelleBehandlingerRepository.hentAndreIkkeAvsluttedeYtelsesbehandlinger(behandling.getFagsakId(), behandling.getId())) {
            Behandling venter = behandlingRepository.hentBehandling(annen.behandlingId());
            if (venter.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_PARALLELL_BEHANDLING)) {
                log.info("Behandling {} hoppet tilbake fra {} til {} og holder ikke lenger porten. Gjenopptar behandling {}",
                    behandling.getId(), fraSteg.getKode(), tilSteg.getKode(), venter.getId());
                behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(venter, false);
            }
        }
    }

    /**
     * Behandlingen slipper porten når den går fra et steg i eller etter {@link ParallelleBehandlingerTjeneste#FØRSTE_STEG_ETTER_PORTEN}
     * til et steg før. Porten selv sammenlignes aldri i modellen, siden den mangler i modellene for førstegangsbehandling
     * og revurdering når bryteren er av.
     */
    private boolean slipperPorten(Behandling behandling, BehandlingStegType fraSteg, BehandlingStegType tilSteg) {
        var ytelseType = behandling.getFagsakYtelseType();
        var behandlingType = behandling.getType();
        var referanse = ParallelleBehandlingerTjeneste.FØRSTE_STEG_ETTER_PORTEN;
        boolean varForbiPorten = behandlingskontrollTjeneste.sammenlignRekkefølge(ytelseType, behandlingType, fraSteg, referanse) >= 0;
        boolean erFørPorten = tilSteg == PORTEN || behandlingskontrollTjeneste.sammenlignRekkefølge(ytelseType, behandlingType, tilSteg, referanse) < 0;
        return varForbiPorten && erFørPorten;
    }
}
