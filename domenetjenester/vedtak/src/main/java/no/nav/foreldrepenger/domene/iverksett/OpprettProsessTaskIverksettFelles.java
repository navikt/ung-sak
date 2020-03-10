package no.nav.foreldrepenger.domene.iverksett;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.vedtak.ekstern.VurderOppgaveArenaTask;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.økonomi.SendØkonomiOppdragTask;
import no.nav.foreldrepenger.økonomi.task.SendTilkjentYtelseTask;
import no.nav.foreldrepenger.økonomi.task.VurderOppgaveTilbakekrevingTask;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public abstract class OpprettProsessTaskIverksettFelles implements OpprettProsessTaskIverksett {

    protected ProsessTaskRepository prosessTaskRepository;
    protected OppgaveTjeneste oppgaveTjeneste;

    protected OpprettProsessTaskIverksettFelles() {
        // for CDI proxy
    }

    public OpprettProsessTaskIverksettFelles(ProsessTaskRepository prosessTaskRepository,
                                             OppgaveTjeneste oppgaveTjeneste) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public void opprettIverksettingstasker(Behandling behandling) {
        opprettIverksettingstasker(behandling, Optional.empty());
    }

    @Override
    public void opprettIverksettingstasker(Behandling behandling, Optional<String> initiellTaskNavn) {
        Optional<ProsessTaskData> avsluttOppgave = oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling,
            behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, false);
        Optional<ProsessTaskData> initiellTask = Optional.empty();
        if (initiellTaskNavn.isPresent()) {
            initiellTask = Optional.of(new ProsessTaskData(initiellTaskNavn.get()));
        }
        ProsessTaskGruppe taskData = new ProsessTaskGruppe();
        initiellTask.ifPresent(taskData::addNesteSekvensiell);

        List<ProsessTaskData> parallelle = new ArrayList<>();
        parallelle.add(new ProsessTaskData(SendVedtaksbrevTask.TASKTYPE));
        parallelle.add(opprettTaskSendTilØkonomi());
        avsluttOppgave.ifPresent(parallelle::add);

        taskData.addNesteParallell(parallelle);

        taskData.addNesteSekvensiell(new ProsessTaskData(SendTilkjentYtelseTask.TASKTYPE));

        taskData.addNesteSekvensiell(new ProsessTaskData(VurderOppgaveArenaTask.TASKTYPE));
        taskData.addNesteSekvensiell(new ProsessTaskData(AvsluttBehandlingTask.TASKTYPE));

        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        taskData.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(taskData);

        // Opprettes som egen task da den er uavhengig av de andre
        prosessTaskRepository.lagre(opprettTaskVurderOppgaveTilbakekreving(behandling));
    }

    private ProsessTaskData opprettTaskSendTilØkonomi() {
        ProsessTaskData taskdata = new ProsessTaskData(SendØkonomiOppdragTask.TASKTYPE);
        OffsetDateTime nå = OffsetDateTime.now(ZoneId.of("UTC"));
        taskdata.setProperty("opprinneligIversettingTidspunkt", nå.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return taskdata;
    }

    private ProsessTaskData opprettTaskVurderOppgaveTilbakekreving(Behandling behandling) {
        ProsessTaskData vurderOppgaveTilbakekreving = new ProsessTaskData(VurderOppgaveTilbakekrevingTask.TASKTYPE);
        vurderOppgaveTilbakekreving.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        vurderOppgaveTilbakekreving.setCallIdFraEksisterende();
        return vurderOppgaveTilbakekreving;
    }
}
