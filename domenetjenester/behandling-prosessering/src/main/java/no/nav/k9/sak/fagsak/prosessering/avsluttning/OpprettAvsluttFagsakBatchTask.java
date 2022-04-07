package no.nav.k9.sak.fagsak.prosessering.avsluttning;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
@ProsessTask(value = "batch.finnFagsakerForAvsluttning", maxFailedRuns = 1)
public class OpprettAvsluttFagsakBatchTask implements ProsessTaskHandler {

    private FagsakAvsluttningTjeneste tjeneste;
    private ProsessTaskTjeneste prosessTaskRepository;

    OpprettAvsluttFagsakBatchTask() {
        // CDI
    }

    @Inject
    public OpprettAvsluttFagsakBatchTask(FagsakAvsluttningTjeneste tjeneste, ProsessTaskTjeneste prosessTaskRepository) {
        this.tjeneste = tjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public static ProsessTaskData opprettTask(List<Saksnummer> saksnummer) {
        if (saksnummer.isEmpty()) {
            return null;
        }
        var payload = saksnummer.stream().map(Saksnummer::getVerdi).collect(Collectors.joining(","));

        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettAvsluttFagsakBatchTask.class);
        prosessTaskData.setPayload(payload);
        return prosessTaskData;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var saksnummer = Stream.of(prosessTaskData.getPayloadAsString().split(","));

        saksnummer.map(String::trim)
            .map(Saksnummer::new)
            .map(it -> tjeneste.finnFagsakForSaksnummer(it))
            .map(AvsluttFagsakTask::opprettTask)
            .forEach(taskdata -> prosessTaskRepository.lagre(taskdata));
    }
}
