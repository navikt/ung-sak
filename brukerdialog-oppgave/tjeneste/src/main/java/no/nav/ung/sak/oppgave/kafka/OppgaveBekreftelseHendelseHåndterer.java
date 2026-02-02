package no.nav.ung.sak.oppgave.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.oppgave.kafka.model.UngdomsytelseOppgavebekreftelseTopicEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ApplicationScoped
@ActivateRequestContext
@Transactional
public class OppgaveBekreftelseHendelseHåndterer {

    private static final Logger log = LoggerFactory.getLogger(OppgaveBekreftelseHendelseHåndterer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProsessTaskTjeneste taskRepository;

    OppgaveBekreftelseHendelseHåndterer() {
    }

    @Inject
    public OppgaveBekreftelseHendelseHåndterer(ProsessTaskTjeneste taskRepository) {
        this.taskRepository = taskRepository;
    }

    void handleMessage(String key, String payload) {
        log.info("Mottatt bekreftelse på oppgave med key='{}', payload={}", key, payload);

        try {
            var topicEntry = MAPPER.readValue(payload, UngdomsytelseOppgavebekreftelseTopicEntry.class);
            var oppgavebekreftelse = topicEntry.data().journalførtMelding();
            var journalpostId = oppgavebekreftelse.journalpostId();

            log.info("Behandler oppgavebekreftelse for journalpostId='{}'",
                journalpostId);

            // Opprett prosesstask for å håndtere oppgavebekreftelsen
            var prosessTaskData = ProsessTaskData.forProsessTask(OppgaveBekreftelseProsessTask.class);
            prosessTaskData.setPayload(payload);
            prosessTaskData.setCallIdFraEksisterende();

            taskRepository.lagre(prosessTaskData);

            log.info("Opprettet prosesstask for oppgavebekreftelse med journalpostId='{}'", journalpostId);

        } catch (Exception e) {
            log.error("Feil ved håndtering av oppgavebekreftelse med key='{}', payload={}", key, payload, e);
            throw new RuntimeException("Feil ved håndtering av oppgavebekreftelse", e);
        }
    }

}
