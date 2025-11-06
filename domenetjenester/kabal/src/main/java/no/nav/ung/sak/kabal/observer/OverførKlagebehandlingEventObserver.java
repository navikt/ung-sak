package no.nav.ung.sak.kabal.observer;

import java.util.List;
import java.util.Set;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingskontrollEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kabal.task.OverføringTilKabalTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;

/**
 * Observerer behandlinger med åpne aksjonspunkter og overfører til Kabal
 */
@ApplicationScoped
public class OverførKlagebehandlingEventObserver {

    private static final Logger logger = LoggerFactory.getLogger(OverførKlagebehandlingEventObserver.class);

    private ProsessTaskTjeneste prosessTaskRepository;
    private BehandlingRepository behandlingRepository;
    private Boolean lansert;

    @Inject
    public OverførKlagebehandlingEventObserver(ProsessTaskTjeneste prosessTaskRepository, BehandlingRepository behandlingRepository,
                                               @KonfigVerdi(value = "KLAGE_ENABLED", defaultVerdi = "false") Boolean lansert) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlingRepository = behandlingRepository;
        this.lansert = lansert;
    }

    /**
     * Overfør behandling til NK (system Kabal) etter at behandlingskontroll er kjørt ferdig.
     */
    public void overførBehandlingTilKabal(@Observes BehandlingskontrollEvent.StoppetEvent event) {
        Behandling behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        if (behandling.erYtelseBehandling()) {
            return;
        }

        if (!lansert) {
            logger.info("Overføring til Kabal er ikke lansert, avslutter uten å opprette task");
            return;
        }

        List<Aksjonspunkt> åpneAutopunkter = behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT);
        var inneholderAutopunktForVentingPåKabal = inneholderAutopunkt(event, AksjonspunktDefinisjon.AUTO_OVERFØRT_NK, åpneAutopunkter);
        if (inneholderAutopunktForVentingPåKabal) {
            // Overfør klagebehandling til NAV Klageinstans
            overførKlagebehandlingTilKabal(behandling);
        }
    }

    private void overførKlagebehandlingTilKabal(Behandling behandling) {
        logger.info("Overfører klagebehandling til Kabal");

        ProsessTaskData taskData = ProsessTaskData.forProsessTask(OverføringTilKabalTask.class);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskData.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(taskData);
    }

    private boolean inneholderAutopunkt(BehandlingskontrollEvent event, AksjonspunktDefinisjon aksjonspunktDefinisjon, List<Aksjonspunkt> åpneAksjonspunkter) {
        Set<String> aksjonspunktForSteg = event.getBehandlingModell().finnAksjonspunktDefinisjoner(event.getStegType());
        return åpneAksjonspunkter.stream()
            .filter(ad -> aksjonspunktForSteg.contains(ad.getAksjonspunktDefinisjon().getKode()))
            .anyMatch(ad -> ad.getAksjonspunktDefinisjon() == aksjonspunktDefinisjon);
    }
}
