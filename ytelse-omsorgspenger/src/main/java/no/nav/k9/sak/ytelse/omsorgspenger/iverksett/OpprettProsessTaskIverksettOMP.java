package no.nav.k9.sak.ytelse.omsorgspenger.iverksett;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksettTilkjentYtelseFelles;
import no.nav.k9.sak.hendelse.stønadstatistikk.StønadstatistikkService;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.ytelse.overlapp.VurderOverlappendeYtelserTask;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumDeaktiveringTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OpprettProsessTaskIverksettOMP extends OpprettProsessTaskIverksettTilkjentYtelseFelles {

    private ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste;

    OpprettProsessTaskIverksettOMP() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksettOMP(FagsakProsessTaskRepository prosessTaskRepository,
                                          OppgaveTjeneste oppgaveTjeneste,
                                          InfotrygdFeedService infotrygdFeedService,
                                          ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste,
                                          StønadstatistikkService stønadstatistikkService) {
        super(prosessTaskRepository, oppgaveTjeneste, infotrygdFeedService, stønadstatistikkService);
        this.årskvantumDeaktiveringTjeneste = årskvantumDeaktiveringTjeneste;

    }

    @Override
    public void opprettYtelsesSpesifikkeTasks(Behandling behandling) {
        årskvantumDeaktiveringTjeneste.meldFraDersomDeaktivering(behandling);

        var taskData = new ProsessTaskData(VurderOverlappendeYtelserTask.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        fagsakProsessTaskRepository.lagreNyGruppe(taskData);
    }
}
