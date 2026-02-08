package no.nav.ung.sak.oppgave.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.kontrakt.oppgaver.BekreftelseDTO;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveRepository;
import no.nav.ung.sak.oppgave.OppgaveLivssyklusTjeneste;
import no.nav.ung.sak.oppgave.kafka.model.UngdomsytelseOppgavebekreftelseTopicEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * ProsessTask for å håndtere oppgavebekreftelse mottatt fra Kafka.
 */
@ApplicationScoped
@ProsessTask(value = OppgaveBekreftelseProsessTask.TASK_NAVN)
public class OppgaveBekreftelseProsessTask implements ProsessTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(OppgaveBekreftelseProsessTask.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String TASK_NAVN = "handle.oppgave.bekreftelse";

    private BrukerdialogOppgaveRepository oppgaveRepository;
    private OppgaveLivssyklusTjeneste oppgaveLivssyklusTjeneste;

    OppgaveBekreftelseProsessTask() {
        // CDI
    }

    @Inject
    public OppgaveBekreftelseProsessTask(BrukerdialogOppgaveRepository oppgaveRepository, OppgaveLivssyklusTjeneste oppgaveLivssyklusTjeneste) {
        this.oppgaveRepository = oppgaveRepository;
        this.oppgaveLivssyklusTjeneste = oppgaveLivssyklusTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var payload = prosessTaskData.getPayloadAsString();

        UngdomsytelseOppgavebekreftelseTopicEntry topicEntry = null;
        try {
            topicEntry = MAPPER.readValue(payload, UngdomsytelseOppgavebekreftelseTopicEntry.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Ugyldig payload", e);
        }
        var oppgavebekreftelse = topicEntry.data().journalførtMelding();
        var journalpostId = oppgavebekreftelse.journalpostId();
        var oppgavereferanse = UUID.fromString(oppgavebekreftelse.oppgaveBekreftelse().getSøknadId().getId());
        var bekreftelse = oppgavebekreftelse.oppgaveBekreftelse().getBekreftelse();

        log.info("Behandler oppgavebekreftelse for journalpostId='{}', oppgavereferanse='{}'",
            journalpostId, oppgavereferanse);

        // Finn oppgaven basert på oppgavereferanse
        var oppgave = oppgaveRepository.hentOppgaveForOppgavereferanse(oppgavereferanse)
            .orElseThrow(() -> new IllegalStateException(
                "Fant ingen oppgave for oppgavereferanse=" + oppgavereferanse));

        // Oppdater oppgaven med bekreftelse
        oppgave.setBekreftelse(new BekreftelseDTO(bekreftelse.harUttalelse(), bekreftelse.getUttalelseFraBruker()));
        oppgaveLivssyklusTjeneste.løsOppgave(oppgave);

        log.info("Oppgavebekreftelse behandlet for oppgave med referanse='{}'",
            oppgave.getOppgavereferanse());

    }
}

