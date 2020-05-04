package no.nav.k9.sak.domene.iverksett;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.vedtak.ekstern.VurderOppgaveArenaTask;
import no.nav.k9.sak.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.k9.sak.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.økonomi.SendØkonomiOppdragTask;
import no.nav.k9.sak.økonomi.task.VurderOppgaveTilbakekrevingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class OpprettProsessTaskIverksettFelles implements OpprettProsessTaskIverksett {

    protected ProsessTaskRepository prosessTaskRepository;
    protected OppgaveTjeneste oppgaveTjeneste;
    protected InfotrygdFeedService infotrygdFeedService;

    protected OpprettProsessTaskIverksettFelles() {
        // for CDI proxy
    }

    public OpprettProsessTaskIverksettFelles(ProsessTaskRepository prosessTaskRepository,
                                             OppgaveTjeneste oppgaveTjeneste,
                                             InfotrygdFeedService infotrygdFeedService) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.infotrygdFeedService = infotrygdFeedService;
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
        parallelle.add(opprettTaskSendTilØkonomi());
        avsluttOppgave.ifPresent(parallelle::add);

        taskData.addNesteParallell(parallelle);

        // FIXME: Antar at denne er dekket av opprettTaskSendTilØkonomi() ?
        // Da denne sender tilkjent ytelse til fp.oppdrag via kafka
        // taskData.addNesteSekvensiell(new ProsessTaskData(SendTilkjentYtelseTask.TASKTYPE));

        if (skalVurdereOppgaveTilArena(behandling)) {
            taskData.addNesteSekvensiell(new ProsessTaskData(VurderOppgaveArenaTask.TASKTYPE));
        }
        taskData.addNesteSekvensiell(new ProsessTaskData(AvsluttBehandlingTask.TASKTYPE));

        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        taskData.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(taskData);

        // Opprettes som egen task da den er uavhengig av de andre
        prosessTaskRepository.lagre(opprettTaskVurderOppgaveTilbakekreving(behandling));

        infotrygdFeedService.publiserHendelse(behandling);
    }

    private boolean skalVurdereOppgaveTilArena(Behandling behandling) {
        // varsle Arena for andre ytelser enn FRISINN
        return !(FagsakYtelseType.FRISINN.equals(behandling.getFagsakYtelseType()));
    }

    private ProsessTaskData opprettTaskSendTilØkonomi() {
        ProsessTaskData taskdata = new ProsessTaskData(SendØkonomiOppdragTask.TASKTYPE);
        OffsetDateTime nå = OffsetDateTime.now(ZoneId.of("UTC"));
        taskdata.setProperty("opprinneligIverksettingTidspunkt", nå.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return taskdata;
    }

    private ProsessTaskData opprettTaskVurderOppgaveTilbakekreving(Behandling behandling) {
        ProsessTaskData vurderOppgaveTilbakekreving = new ProsessTaskData(VurderOppgaveTilbakekrevingTask.TASKTYPE);
        vurderOppgaveTilbakekreving.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        vurderOppgaveTilbakekreving.setCallIdFraEksisterende();
        return vurderOppgaveTilbakekreving;
    }
}
