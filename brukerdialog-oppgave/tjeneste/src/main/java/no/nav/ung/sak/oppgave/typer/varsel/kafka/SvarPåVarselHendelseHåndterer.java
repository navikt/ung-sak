package no.nav.ung.sak.oppgave.typer.varsel.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.JsonObjectMapper;
import no.nav.ung.sak.oppgave.typer.varsel.kafka.model.SvarPåVarselTopicEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ApplicationScoped
@ActivateRequestContext
@Transactional
public class SvarPåVarselHendelseHåndterer {

    private static final Logger log = LoggerFactory.getLogger(SvarPåVarselHendelseHåndterer.class);

    private ProsessTaskTjeneste taskRepository;

    SvarPåVarselHendelseHåndterer() {
    }

    @Inject
    public SvarPåVarselHendelseHåndterer(ProsessTaskTjeneste taskRepository) {
        this.taskRepository = taskRepository;
    }

    void handleMessage(String payload) {
        try {
            var topicEntry = JsonObjectMapper.fromJson(payload, SvarPåVarselTopicEntry.class);
            var oppgavebekreftelse = topicEntry.data().journalførtMelding();
            var journalpostId = oppgavebekreftelse.journalpostId();

            log.info("Behandler oppgavebekreftelse for journalpostId='{}'",
                journalpostId);

            // Opprett prosesstask for å håndtere svaret
            var prosessTaskData = ProsessTaskData.forProsessTask(SvarPåVarselProsessTask.class);
            prosessTaskData.setPayload(payload);
            prosessTaskData.setCallIdFraEksisterende();

            taskRepository.lagre(prosessTaskData);

            log.info("Opprettet prosesstask for svar på varsel med journalpostId='{}'", journalpostId);

        } catch (Exception e) {
            throw new IllegalStateException("Feil ved håndtering av svar p varsel", e);
        }
    }

}
