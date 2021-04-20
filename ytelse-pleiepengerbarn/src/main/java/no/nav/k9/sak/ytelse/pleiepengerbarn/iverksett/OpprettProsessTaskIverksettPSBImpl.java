package no.nav.k9.sak.ytelse.pleiepengerbarn.iverksett;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksettTilkjentYtelseFelles;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;


@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class OpprettProsessTaskIverksettPSBImpl extends OpprettProsessTaskIverksettTilkjentYtelseFelles {

    OpprettProsessTaskIverksettPSBImpl() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksettPSBImpl(FagsakProsessTaskRepository prosessTaskRepository,
                                              OppgaveTjeneste oppgaveTjeneste,
                                              InfotrygdFeedService infotrygdFeedService) {
        super(prosessTaskRepository, oppgaveTjeneste, infotrygdFeedService);
    }

    @Override
    public void opprettYtelsesSpesifikkeTasks(Behandling behandling) {
        var taskData = new ProsessTaskData(VurderRevurderingAndreSøknaderTask.TASKNAME);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        fagsakProsessTaskRepository.lagreNyGruppe(taskData);
    }
}
