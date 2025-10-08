package no.nav.ung.sak.metrikker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.metrikker.bigquery.KontrollerteInntektPerioderMetrikkPubliserer;


// ung-tilbake lytter på denne for å opprette tilbakekrevingsbehandlinger ved behov
@ApplicationScoped
@ProsessTask(PubliserKontrollerteInntektperioderMetrikkTask.TASKTYPE)
public class PubliserKontrollerteInntektperioderMetrikkTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "bigquery.kontrollertinntektmetrikk";
    private KontrollerteInntektPerioderMetrikkPubliserer kontrollerteInntektPerioderMetrikkPubliserer;
    private BehandlingRepository behandlingRepository;

    PubliserKontrollerteInntektperioderMetrikkTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserKontrollerteInntektperioderMetrikkTask(KontrollerteInntektPerioderMetrikkPubliserer kontrollerteInntektPerioderMetrikkPubliserer, BehandlingRepository behandlingRepository) {
        this.kontrollerteInntektPerioderMetrikkPubliserer = kontrollerteInntektPerioderMetrikkPubliserer;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String behandingIdString = prosessTaskData.getBehandlingId();

        if (behandingIdString != null && !behandingIdString.isEmpty()) {
            long behandlingId = Long.parseLong(behandingIdString);
            Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
            kontrollerteInntektPerioderMetrikkPubliserer.publiserKontrollertePerioderMetrikker(BehandlingReferanse.fra(behandling));
        }
    }


}
