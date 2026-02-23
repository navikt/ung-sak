package no.nav.ung.sak.oppgave.typer.varsel.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.JsonObjectMapper;
import no.nav.ung.sak.kontrakt.oppgaver.SvarPåVarselDTO;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveRepository;
import no.nav.ung.sak.oppgave.OppgaveLivssyklusTjeneste;
import no.nav.ung.sak.oppgave.typer.varsel.kafka.model.SvarPåVarselTopicEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * ProsessTask for å håndtere oppgavebekreftelse mottatt fra Kafka.
 */
@ApplicationScoped
@ProsessTask(value = SvarPåVarselProsessTask.TASK_NAVN)
public class SvarPåVarselProsessTask implements ProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(SvarPåVarselProsessTask.class);
    private static final ObjectMapper MAPPER = JsonObjectMapper.getMapper();
    public static final String TASK_NAVN = "handle.varsel.uttalelse";

    private BrukerdialogOppgaveRepository oppgaveRepository;
    private OppgaveLivssyklusTjeneste oppgaveLivssyklusTjeneste;

    SvarPåVarselProsessTask() {
        // CDI
    }

    @Inject
    public SvarPåVarselProsessTask(BrukerdialogOppgaveRepository oppgaveRepository, OppgaveLivssyklusTjeneste oppgaveLivssyklusTjeneste) {
        this.oppgaveRepository = oppgaveRepository;
        this.oppgaveLivssyklusTjeneste = oppgaveLivssyklusTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var payload = prosessTaskData.getPayloadAsString();

        SvarPåVarselTopicEntry topicEntry = null;
        try {
            topicEntry = MAPPER.readValue(payload, SvarPåVarselTopicEntry.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Ugyldig payload", e);
        }
        var svar = topicEntry.data().journalførtMelding();
        var journalpostId = svar.journalpostId();
        var oppgavereferanse = UUID.fromString(svar.oppgaveBekreftelse().getSøknadId().getId());
        var bekreftelse = svar.oppgaveBekreftelse().getBekreftelse();

        log.info("Behandler svar på varsel for journalpostId='{}', oppgavereferanse='{}'",
            journalpostId, oppgavereferanse);

        // Finn oppgaven basert på oppgavereferanse
        var oppgave = oppgaveRepository.hentOppgaveForOppgavereferanse(oppgavereferanse)
            .orElseThrow(() -> new IllegalStateException(
                "Fant ingen oppgave for oppgavereferanse=" + oppgavereferanse));

        // Oppdater oppgaven med bekreftelse
        oppgave.setBekreftelse(new SvarPåVarselDTO(bekreftelse.harUttalelse(), bekreftelse.getUttalelseFraBruker()));
        oppgaveLivssyklusTjeneste.løsOppgave(oppgave);

        log.info("Svar på varsel behandlet for oppgave med referanse='{}'",
            oppgave.getOppgavereferanse());

    }
}

