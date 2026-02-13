package no.nav.ung.sak.oppgave.typer.varsel.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.JsonObjectMapper;
import no.nav.ung.sak.oppgave.kafka.KafkaMessageHandler;
import no.nav.ung.sak.oppgave.typer.varsel.kafka.model.SvarPåVarselTopicEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ApplicationScoped
@ActivateRequestContext
@Transactional
public class SvarPåVarselHendelseHåndterer implements KafkaMessageHandler.KafkaStringMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(SvarPåVarselHendelseHåndterer.class);

    private static final String GROUP_ID = "ung-varsel-bekreftelse"; // Hold konstant pga offset commit
    private boolean oppgaverIUngsakEnabled;
    private String topicName;
    private ProsessTaskTjeneste taskTjeneste;

    SvarPåVarselHendelseHåndterer() {
    }

    @Inject
    public SvarPåVarselHendelseHåndterer(
        @KonfigVerdi(value = "OPPGAVER_I_UNGSAK_ENABLED", defaultVerdi = "true") boolean oppgaverIUngsakEnabled,
        @KonfigVerdi(value = "KAFKA_OPPGAVEBEKREFTELSE_TOPIC", defaultVerdi = "dusseldorf.ungdomsytelse-oppgavebekreftelse-cleanup") String topicName,
        ProsessTaskTjeneste taskTjeneste) {
        this.oppgaverIUngsakEnabled = oppgaverIUngsakEnabled;
        this.topicName = topicName;
        this.taskTjeneste = taskTjeneste;
    }


    @Override
    public void handleRecord(String key, String value) {
        try {
            var topicEntry = JsonObjectMapper.fromJson(value, SvarPåVarselTopicEntry.class);
            var oppgavebekreftelse = topicEntry.data().journalførtMelding();
            var journalpostId = oppgavebekreftelse.journalpostId();

            log.info("Behandler svar på varsel for journalpostId='{}'",
                journalpostId);

            // Opprett prosesstask for å håndtere svaret
            var prosessTaskData = ProsessTaskData.forProsessTask(SvarPåVarselProsessTask.class);
            prosessTaskData.setPayload(value);
            prosessTaskData.setCallIdFraEksisterende();

            taskTjeneste.lagre(prosessTaskData);

            log.info("Opprettet prosesstask for svar på varsel med journalpostId='{}'", journalpostId);

        } catch (Exception e) {
            throw new IllegalStateException("Feil ved håndtering av svar på varsel", e);
        }
    }

    @Override
    public String topic() {
        return topicName;
    }

    @Override
    public boolean enabled() {
        return oppgaverIUngsakEnabled;
    }

    @Override
    public String groupId() {
        return GROUP_ID;
    }

}
