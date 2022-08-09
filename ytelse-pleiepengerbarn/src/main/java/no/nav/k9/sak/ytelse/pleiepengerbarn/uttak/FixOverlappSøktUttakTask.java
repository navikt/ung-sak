package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;

@ApplicationScoped
@ProsessTask(FixOverlappSøktUttakTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class FixOverlappSøktUttakTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "forvaltning.fix.overlapp.sokt.uttak";
    private static final Logger logger = LoggerFactory.getLogger(FixOverlappSøktUttakTask.class);

    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    @Inject
    public FixOverlappSøktUttakTask(BehandlingRepository repository, BehandlingLåsRepository behandlingLåsRepository, MapInputTilUttakTjeneste mapInputTilUttakTjeneste, UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository) {
        super(repository, behandlingLåsRepository);
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
    }

    @Override
    protected void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        if (erRiktigBehandling(behandling)) {
            uttakPerioderGrunnlagRepository.dedupliserSøktUttak(behandling.getId());
        }
    }

    private boolean erRiktigBehandling(Behandling behandling) {
        BehandlingReferanse behandlingReferanse = BehandlingReferanse.fra(behandling);
        try {
            mapInputTilUttakTjeneste.hentUtOgMapRequest(behandlingReferanse);
        } catch (Exception e) {
            if (e.getMessage().contains("Overlapp")){
                return  true;
            }
            logger.warn("Avbryter, behandlingen har ikke overlapp i søkt uttak, fikk Exception", e);
        }
        logger.warn("Avbryter, behandlingen har ikke overlapp i søkt uttak");
        return false;
    }
}
