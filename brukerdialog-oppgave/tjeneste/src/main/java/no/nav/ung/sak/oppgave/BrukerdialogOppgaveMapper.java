package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.oppgave.kontrakt.BrukerdialogOppgaveDto;

@ApplicationScoped
public class BrukerdialogOppgaveMapper {

    public BrukerdialogOppgaveMapper() {
        // CDI proxy
    }

    public BrukerdialogOppgaveDto tilDto(BrukerdialogOppgaveEntitet oppgave) {
        return new BrukerdialogOppgaveDto(
            oppgave.getOppgavereferanse(),
            oppgave.getStatus() != null ? oppgave.getStatus().name() : null,
            oppgave.getOppgaveType() != null ? oppgave.getOppgaveType().name() : null,
            oppgave.getData(),
            oppgave.getFristTid(),
            oppgave.getOpprettetTidspunkt(),
            oppgave.getLøstDato(),
            oppgave.getÅpnetDato(),
            oppgave.getLukketDato()
        );
    }
}

