package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.k9.sak.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.k9.sak.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP_KS")
@FagsakYtelseTypeRef("OMP_MA")
public class UtvidetRettOpprettProsessTaskIverksett implements OpprettProsessTaskIverksett {

    protected FagsakProsessTaskRepository fagsakProsessTaskRepository;
    protected OppgaveTjeneste oppgaveTjeneste;

    protected UtvidetRettOpprettProsessTaskIverksett() {
        // for CDI proxy
    }

    @Inject
    public UtvidetRettOpprettProsessTaskIverksett(FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                                  OppgaveTjeneste oppgaveTjeneste) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public void opprettIverksettingstasker(Behandling behandling) {
        opprettIverksettingstasker(behandling, Optional.empty());
    }

    @Override
    public void opprettIverksettingstasker(Behandling behandling, Optional<String> initiellTaskNavn) {
        Optional<ProsessTaskData> avsluttOppgave = oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling,
            behandling.erRevurdering() ? OppgaveÅrsak.REVURDER_VL : OppgaveÅrsak.BEHANDLE_SAK_VL, false);

        Optional<ProsessTaskData> initiellTask = Optional.empty();
        if (initiellTaskNavn.isPresent()) {
            initiellTask = Optional.of(new ProsessTaskData(initiellTaskNavn.get()));
        }
        ProsessTaskGruppe taskData = new ProsessTaskGruppe();
        initiellTask.ifPresent(taskData::addNesteSekvensiell);

        List<ProsessTaskData> parallelle = new ArrayList<>();
        parallelle.add(new ProsessTaskData(SendVedtaksbrevTask.TASKTYPE));
        avsluttOppgave.ifPresent(parallelle::add);

        taskData.addNesteParallell(parallelle);

        taskData.addNesteSekvensiell(new ProsessTaskData(AvsluttBehandlingTask.TASKTYPE));

        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        taskData.setCallIdFraEksisterende();

        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId.toString(), taskData);

        opprettYtelsesSpesifikkeTasks(behandling);
    }

}
