package no.nav.ung.sak.kabal.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingAnsvarlig;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingAnsvarligTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.kabal.rest.KabalRestKlient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ProsessTask(OverføringTilKabalTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OverføringTilKabalTask extends BehandlingProsessTask {
    public static final String TASKTYPE = "kabal.overfoer_klagebehandling";
    private static final Logger log = LoggerFactory.getLogger(OverføringTilKabalTask.class);

    private KabalRestKlient restKlient;
    private KabalRequestMapperV4 kabalRequestMapper;
    private BehandlingRepository behandlingRepository;
    private TpsTjeneste pdlTjeneste;
    private KlageRepository klageRepository;
    private BehandlingAnsvarligTjeneste behandlingAnsvarligTjeneste;

    OverføringTilKabalTask() {
        // for CDI proxy
    }

    @Inject
    public OverføringTilKabalTask(KabalRestKlient restKlient,
                                  BehandlingRepositoryProvider repositoryProvider,
                                  KabalRequestMapperV4 kabalRequestMapper,
                                  BehandlingRepository behandlingRepository,
                                  TpsTjeneste pdlTjeneste,
                                  KlageRepository klageRepository,
                                  BehandlingAnsvarligTjeneste behandlingAnsvarligTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.restKlient = restKlient;
        this.kabalRequestMapper = kabalRequestMapper;
        this.behandlingRepository = behandlingRepository;
        this.pdlTjeneste = pdlTjeneste;
        this.klageRepository = klageRepository;
        this.behandlingAnsvarligTjeneste = behandlingAnsvarligTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());
        BehandlingAnsvarlig behandlingAnsvarlig = behandlingAnsvarligTjeneste.finnBehandlingAnsvarligFraBehandlingTilstand(behandling).orElse(null);
        logContext(behandling);
        var personIdent = pdlTjeneste.hentFnrForAktør(behandling.getAktørId());
        var klageUtredning = klageRepository.hentKlageUtredning(behandling.getId());

        var request = kabalRequestMapper.map(behandling, behandlingAnsvarlig, personIdent, klageUtredning);
        log.info("Overfører til kabal - request={}", request);
        restKlient.overførKlagebehandling(request);
    }
}
