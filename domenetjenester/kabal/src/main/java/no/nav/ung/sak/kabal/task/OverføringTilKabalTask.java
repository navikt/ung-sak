package no.nav.ung.sak.kabal.task;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.kabal.rest.KabalRestKlient;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@ProsessTask(OverføringTilKabalTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OverføringTilKabalTask extends BehandlingProsessTask {
    public static final String TASKTYPE = "kabal.overfoer_klagebehandling";
    private static final Logger log = LoggerFactory.getLogger(OverføringTilKabalTask.class);

    private KabalRestKlient restKlient;
    private KabalRequestMapperV4 kabalRequestMapper;
    private BehandlingRepository repository;
    private TpsTjeneste pdlTjeneste;
    private KlageRepository klageRepository;
    private boolean klageEnabled;


    OverføringTilKabalTask() {
        // for CDI proxy
    }

    @Inject
    public OverføringTilKabalTask(KabalRestKlient restKlient,
                                  BehandlingRepositoryProvider repositoryProvider,
                                  KabalRequestMapperV4 kabalRequestMapper,
                                  BehandlingRepository repository,
                                  TpsTjeneste pdlTjeneste,
                                  KlageRepository klageRepository,
                                  @KonfigVerdi(value = "KLAGE_ENABLED", defaultVerdi = "false") boolean klageEnabled) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.restKlient = restKlient;
        this.kabalRequestMapper = kabalRequestMapper;
        this.repository = repository;
        this.pdlTjeneste = pdlTjeneste;
        this.klageRepository = klageRepository;
        this.klageEnabled = klageEnabled;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandling = repository.hentBehandling(prosessTaskData.getBehandlingId());
        logContext(behandling);
        var personIdent = pdlTjeneste.hentFnrForAktør(behandling.getAktørId());
        var klageUtredning = klageRepository.hentKlageUtredning(behandling.getId());
        var klageVurdering = klageUtredning.hentKlagevurdering(KlageVurdertAv.VEDTAKSINSTANS)
            .orElseThrow(() -> new IllegalStateException("Fann ikke NFP-klageVurdering for klage: " + behandling));

        if (klageEnabled) {
            var request = kabalRequestMapper.map(behandling, personIdent, klageUtredning);
            log.info("Overfører til kabal - request={}", request);
            restKlient.overførKlagebehandling(request);
        }
    }
}
