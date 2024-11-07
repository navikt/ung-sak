package no.nav.k9.sak.økonomi.tilbakekreving.samkjøring;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;

/**
 * Batchservice som finner alle behandlinger som har aksjonspunkt fordi det var en tilbakekrevingsbehandling på samme sak,
 * og avbryter aksjonspunktet hvis tilbakekrevingsbehandlingen er ferdig.
 */
@ApplicationScoped
@ProsessTask(value = GjenopptaVenterPåTilbakekrevingBatchTask.TASKTYPE, cronExpression = "0 5 7 * * *")
public class GjenopptaVenterPåTilbakekrevingBatchTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "batch.gjenopptaVenterPåTilbakekreving";
    private static final Logger logger = LoggerFactory.getLogger(GjenopptaVenterPåTilbakekrevingBatchTask.class);
    private AksjonspunktRepository aksjonspunktRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    GjenopptaVenterPåTilbakekrevingBatchTask() {
        // for CDI proxy
    }

    @Inject
    public GjenopptaVenterPåTilbakekrevingBatchTask(AksjonspunktRepository aksjonspunktRepository, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        List<Behandling> vendtendeBehandlinger = new ArrayList<>();
        vendtendeBehandlinger.addAll(aksjonspunktRepository.hentBehandlingerMedAktivtAksjonspunkt(AksjonspunktDefinisjon.SJEKK_TILBAKEKREVING));
        vendtendeBehandlinger.addAll(aksjonspunktRepository.hentBehandlingerPåVentMedVenteårsak(Venteårsak.VENT_TILBAKEKREVING));

        List<ProsessTaskData> tasker = new ArrayList<>();
        vendtendeBehandlinger.stream()
            .mapToLong(Behandling::getId)
            .distinct()
            .forEach(behandlingId -> {
                    ProsessTaskData task = ProsessTaskData.forProsessTask(GjenopptaVenterPåTilbakekrevingTask.class);
                    task.setProperty("behandlingId", Long.toString(behandlingId));
                    tasker.add(task);
                }
            );

        logger.info("Oppretter {} tasker for å sjekke om behandlinger som venter på tilbakekrevingsbehandling kan fortsette", tasker.size());

        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        gruppe.addNesteParallell(tasker);
        prosessTaskTjeneste.lagre(gruppe);

    }

}
