package no.nav.ung.sak.domene.iverksett;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.ung.sak.hendelse.stønadstatistikk.StønadstatistikkService;
import no.nav.ung.sak.økonomi.SendØkonomiOppdragTask;

@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class UngdomsytelseOpprettProsessTaskIverksett implements OpprettProsessTaskIverksett {

    protected FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private StønadstatistikkService stønadstatistikkService;

    protected UngdomsytelseOpprettProsessTaskIverksett() {
        // for CDI proxy
    }

    @Inject
    public UngdomsytelseOpprettProsessTaskIverksett(FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                                    StønadstatistikkService stønadstatistikkService) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.stønadstatistikkService = stønadstatistikkService;
    }

    @Override
    public void opprettIverksettingstasker(Behandling behandling) {
        opprettIverksettingstasker(behandling, Optional.empty());
    }

    @Override
    public void opprettIverksettingstasker(Behandling behandling, Optional<String> initiellTaskNavn) {
        Optional<ProsessTaskData> initiellTask = Optional.empty();
        if (initiellTaskNavn.isPresent()) {
            initiellTask = Optional.of(ProsessTaskData.forTaskType(new TaskType(initiellTaskNavn.get())));
        }
        ProsessTaskGruppe taskData = new ProsessTaskGruppe();
        initiellTask.ifPresent(taskData::addNesteSekvensiell);

        List<ProsessTaskData> parallelle = new ArrayList<>();
        parallelle.add(opprettTaskSendTilØkonomi());

        taskData.addNesteParallell(parallelle);
        taskData.addNesteSekvensiell(ProsessTaskData.forProsessTask(AvsluttBehandlingTask.class));
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        taskData.setCallIdFraEksisterende();

        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId.toString(), taskData);

        stønadstatistikkService.publiserHendelse(behandling);
    }


    private ProsessTaskData opprettTaskSendTilØkonomi() {
        ProsessTaskData taskdata = ProsessTaskData.forProsessTask(SendØkonomiOppdragTask.class);
        OffsetDateTime nå = OffsetDateTime.now(ZoneId.of("UTC"));
        taskdata.setProperty("opprinneligIverksettingTidspunkt", nå.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return taskdata;
    }


}
