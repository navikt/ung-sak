package no.nav.ung.sak.web.app.tjenester.brukerdialog;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.kontrakt.oppgave.BrukerdialogOppgaveDto;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.varsel.BrukerdialogVarselEntitet;

@ApplicationScoped
public class BrukerdialogOppgaveMapperService {

    public BrukerdialogOppgaveMapperService() {
        // CDI proxy
    }

    public BrukerdialogOppgaveDto mapOppgaveTilDto(BrukerdialogOppgaveEntitet oppgave) {
        return new BrukerdialogOppgaveDto(
            oppgave.getOppgavereferanse(),
            oppgave.getStatus() != null ? oppgave.getStatus().name() : null,
            oppgave.getOppgaveType() != null ? oppgave.getOppgaveType().name() : null,
            oppgave.getData(),
            oppgave instanceof BrukerdialogVarselEntitet
                ? ((BrukerdialogVarselEntitet) oppgave).getFristTid()
                : null,
            oppgave.getOpprettetTidspunkt(),
            oppgave.getLøstDato(),
            oppgave.getÅpnetDato(),
            oppgave.getLukketDato()
        );
    }
}

