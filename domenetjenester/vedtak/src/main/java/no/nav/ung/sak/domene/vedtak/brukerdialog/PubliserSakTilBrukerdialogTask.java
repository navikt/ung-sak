package no.nav.ung.sak.domene.vedtak.brukerdialog;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ProsessTask(PubliserSakTilBrukerdialogTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class PubliserSakTilBrukerdialogTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "publiser.vedtak.brukerdialog";

    private static final Logger log = LoggerFactory.getLogger(PubliserSakTilBrukerdialogTask.class);

    private BehandlingRepository behandlingRepository;
    private BrukerdialogFagsakUtleder brukerdialogFagsakUtleder;
    private UngBrukerdialogSakKlient klient;

    public PubliserSakTilBrukerdialogTask() {
        // for CDI proxy
    }

    @Inject
    public PubliserSakTilBrukerdialogTask(BehandlingLåsRepository behandlingLåsRepository,
                                          BehandlingRepository behandlingRepository,
                                          BrukerdialogFagsakUtleder brukerdialogFagsakUtleder,
                                          UngBrukerdialogSakKlient klient) {
        super(behandlingLåsRepository);
        this.behandlingRepository = behandlingRepository;
        this.brukerdialogFagsakUtleder = brukerdialogFagsakUtleder;
        this.klient = klient;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        long behandlingId = Long.parseLong(prosessTaskData.getBehandlingId());

        var ytelseType = behandlingRepository.hentBehandling(behandlingId).getFagsakYtelseType();
        if (!FagsakYtelseType.AKTIVITETSPENGER.equals(ytelseType)) {
            return;
        }

        var request = brukerdialogFagsakUtleder.utled(behandlingId);
        klient.sendVedtaksstatus(request);

        log.info("Sendte vedtaksstatus for behandlingId={} med {} vedtatte perioder og {} behandlede søknader.",
            behandlingId, request.vedtakPerioder().size(), request.mottatteSøknader().size());
    }
}
