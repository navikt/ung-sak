package no.nav.k9.sak.domene.iverksett;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedService;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.domene.vedtak.ekstern.VurderOppgaveArenaTask;
import no.nav.k9.sak.domene.vedtak.ekstern.VurderOverlappendeInfotrygdYtelserTask;
import no.nav.k9.sak.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.k9.sak.domene.vedtak.intern.FastsettPGIPerioderTask;
import no.nav.k9.sak.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.k9.sak.hendelse.stønadstatistikk.StønadstatistikkService;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.økonomi.SendØkonomiOppdragTask;
import no.nav.k9.sak.økonomi.task.VurderOppgaveTilbakekrevingTask;

public abstract class OpprettProsessTaskIverksettTilkjentYtelseFelles implements OpprettProsessTaskIverksett {

    protected FagsakProsessTaskRepository fagsakProsessTaskRepository;
    protected OppgaveTjeneste oppgaveTjeneste;
    protected InfotrygdFeedService infotrygdFeedService;
    private StønadstatistikkService stønadstatistikkService;

    protected OpprettProsessTaskIverksettTilkjentYtelseFelles() {
        // for CDI proxy
    }

    public OpprettProsessTaskIverksettTilkjentYtelseFelles(FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                                           OppgaveTjeneste oppgaveTjeneste,
                                                           InfotrygdFeedService infotrygdFeedService,
                                                           StønadstatistikkService stønadstatistikkService) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.infotrygdFeedService = infotrygdFeedService;
        this.stønadstatistikkService = stønadstatistikkService;
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
            initiellTask = Optional.of(ProsessTaskData.forTaskType(new TaskType(initiellTaskNavn.get())));
        }
        ProsessTaskGruppe taskData = new ProsessTaskGruppe();
        initiellTask.ifPresent(taskData::addNesteSekvensiell);

        List<ProsessTaskData> parallelle = new ArrayList<>();
        parallelle.add(ProsessTaskData.forProsessTask(SendVedtaksbrevTask.class));
        parallelle.add(opprettTaskSendTilØkonomi());
        avsluttOppgave.ifPresent(parallelle::add);

        taskData.addNesteParallell(parallelle);

        // FIXME: Antar at denne er dekket av opprettTaskSendTilØkonomi() ?
        // Da denne sender tilkjent ytelse til fp.oppdrag via kafka
        // taskData.addNesteSekvensiell( ProsessTaskData.forProsessTask(SendTilkjentYtelseTask.TASKTYPE));

        if (behandling.getFagsakYtelseType().omfattesAvK8()) {
            taskData.addNesteSekvensiell(ProsessTaskData.forProsessTask(FastsettPGIPerioderTask.class));
        }

        taskData.addNesteSekvensiell(ProsessTaskData.forProsessTask(VurderOverlappendeInfotrygdYtelserTask.class));
        if (skalVurdereOppgaveTilArena(behandling)) {
            taskData.addNesteSekvensiell(ProsessTaskData.forProsessTask(VurderOppgaveArenaTask.class));
        }

        taskData.addNesteSekvensiell(ProsessTaskData.forProsessTask(AvsluttBehandlingTask.class));

        opprettYtelsesSpesifikkeTasks(behandling).ifPresent(taskData::addNesteSekvensiell);

        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        taskData.setCallIdFraEksisterende();

        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId.toString(), taskData);

        // Opprettes som egen task da den er uavhengig av de andre
        fagsakProsessTaskRepository.lagreNyGruppe(opprettTaskVurderOppgaveTilbakekreving(behandling));

        infotrygdFeedService.publiserHendelse(behandling);
        stønadstatistikkService.publiserHendelse(behandling);
    }

    private boolean skalVurdereOppgaveTilArena(Behandling behandling) {


        // varsle Arena for andre ytelser enn FRISINN
        // FIXME K9: varsler heller ikke Arena dersom Omsorgspenger (ennå sålenge) - til VARIANT FILTERT åpnes.
        var skipYtelser = Set.of(FagsakYtelseType.FRISINN, FagsakYtelseType.OMSORGSPENGER);
        return !(skipYtelser.contains(behandling.getFagsakYtelseType()));
    }

    private ProsessTaskData opprettTaskSendTilØkonomi() {
        ProsessTaskData taskdata = ProsessTaskData.forProsessTask(SendØkonomiOppdragTask.class);
        OffsetDateTime nå = OffsetDateTime.now(ZoneId.of("UTC"));
        taskdata.setProperty("opprinneligIverksettingTidspunkt", nå.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return taskdata;
    }

    private ProsessTaskData opprettTaskVurderOppgaveTilbakekreving(Behandling behandling) {
        ProsessTaskData vurderOppgaveTilbakekreving = ProsessTaskData.forProsessTask(VurderOppgaveTilbakekrevingTask.class);
        vurderOppgaveTilbakekreving.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        vurderOppgaveTilbakekreving.setCallIdFraEksisterende();
        return vurderOppgaveTilbakekreving;
    }
}
