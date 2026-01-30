package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.oppgave.kontrakt.BrukerdialogOppgaveDto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@ApplicationScoped
public class BrukerdialogOppgaveMapper {

    public BrukerdialogOppgaveMapper() {
        // CDI proxy
    }

    public BrukerdialogOppgaveDto tilDto(BrukerdialogOppgaveEntitet oppgave) {
        return new BrukerdialogOppgaveDto(
            oppgave.getOppgavereferanse(),
            oppgave.getOppgaveType(),
            oppgave.getData(),
            oppgave.getBekreftelse(),
            oppgave.getStatus(),
            toZonedDateTime(oppgave.getOpprettetTidspunkt()),
            toZonedDateTime(oppgave.getLøstDato()),
            toZonedDateTime(oppgave.getÅpnetDato()),
            toZonedDateTime(oppgave.getLukketDato()),
            toZonedDateTime(oppgave.getFristTid())
        );
    }


    private ZonedDateTime toZonedDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault());
    }
}

