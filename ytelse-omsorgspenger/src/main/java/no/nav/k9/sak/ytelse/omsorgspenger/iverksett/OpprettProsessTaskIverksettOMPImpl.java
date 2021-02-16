package no.nav.k9.sak.ytelse.omsorgspenger.iverksett;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksettTilkjentYtelseFelles;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.ytelse.overlapp.VurderOverlappendeYtelserTask;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumDeaktiveringTjenesteImpl;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OpprettProsessTaskIverksettOMPImpl extends OpprettProsessTaskIverksettTilkjentYtelseFelles {

    private ÅrskvantumDeaktiveringTjenesteImpl årskvantumDeaktiveringTjeneste;

    OpprettProsessTaskIverksettOMPImpl() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksettOMPImpl(FagsakProsessTaskRepository prosessTaskRepository,
                                              OppgaveTjeneste oppgaveTjeneste,
                                              InfotrygdFeedService infotrygdFeedService,
                                              ÅrskvantumDeaktiveringTjenesteImpl årskvantumDeaktiveringTjeneste) {
        super(prosessTaskRepository, oppgaveTjeneste, infotrygdFeedService);
        this.årskvantumDeaktiveringTjeneste = årskvantumDeaktiveringTjeneste;

    }

    @Override
    public void opprettYtelsesSpesifikkeTasks(Behandling behandling) {
        var taskData = new ProsessTaskData(VurderOverlappendeYtelserTask.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        fagsakProsessTaskRepository.lagreNyGruppe(taskData);
        årskvantumDeaktiveringTjeneste.meldIfraOmIverksetting(behandling);
    }
}
