package no.nav.ung.sak.web.app.tjenester.brukerdialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.JsonObjectMapper;
import no.nav.ung.sak.kontrakt.oppgaver.MigrerOppgaveDto;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * ProsessTask for å migrere en enkelt brukerdialogoppgave fra en annen applikasjon.
 * Idempotent - gjør ingenting hvis oppgave med samme referanse allerede eksisterer.
 */
@ApplicationScoped
@ProsessTask(MigrerBrukerdialogOppgaveTask.TASKTYPE)
public class MigrerBrukerdialogOppgaveTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "brukerdialog.migrerOppgave";
    public static final String OPPGAVE_DATA = "oppgaveData";

    private static final Logger log = LoggerFactory.getLogger(MigrerBrukerdialogOppgaveTask.class);

    private BrukerdialogOppgaveRepository repository;
    private final ObjectMapper objectMapper = JsonObjectMapper.getMapper();

    MigrerBrukerdialogOppgaveTask() {
        // for CDI proxy
    }

    @Inject
    public MigrerBrukerdialogOppgaveTask(BrukerdialogOppgaveRepository repository) {
        this.repository = repository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        try {
            String oppgaveJson = prosessTaskData.getPropertyValue(OPPGAVE_DATA);
            MigrerOppgaveDto oppgaveDto = objectMapper.readValue(oppgaveJson, MigrerOppgaveDto.class);

            // Idempotent sjekk - gjør ingenting hvis oppgaven allerede eksisterer
            var eksisterende = repository.hentOppgaveForOppgavereferanse(oppgaveDto.oppgaveReferanse());
            if (eksisterende.isPresent()) {
                log.info("Oppgave med referanse {} eksisterer allerede, hopper over", oppgaveDto.oppgaveReferanse());
                return;
            }

            // Opprett ny oppgave med alle felter fra migrering
            LocalDateTime frist = oppgaveDto.frist() != null
                ? oppgaveDto.frist().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                : null;

            LocalDateTime løstDato = oppgaveDto.løstDato() != null
                ? oppgaveDto.løstDato().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                : null;

            LocalDateTime åpnetDato = oppgaveDto.åpnetDato() != null
                ? oppgaveDto.åpnetDato().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                : null;

            LocalDateTime lukketDato = oppgaveDto.lukketDato() != null
                ? oppgaveDto.lukketDato().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                : null;

            var oppgave = new BrukerdialogOppgaveEntitet(
                oppgaveDto.oppgaveReferanse(),
                oppgaveDto.oppgavetype(),
                oppgaveDto.aktørId(),
                oppgaveDto.oppgavetypeData(),
                oppgaveDto.bekreftelse(),
                oppgaveDto.status(),
                frist,
                løstDato,
                åpnetDato,
                lukketDato
            );

            repository.persister(oppgave);
            log.info("Migrerte oppgave med referanse {}", oppgaveDto.oppgaveReferanse());

        } catch (Exception e) {
            log.error("Feil ved migrering av oppgave", e);
            throw new RuntimeException("Feil ved migrering av oppgave", e);
        }
    }

    @Override
    public Set<String> requiredProperties() {
        return Set.of(OPPGAVE_DATA);
    }
}

