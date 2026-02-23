package no.nav.ung.sak.oppgave.typer.varsel.typer.endretstartdato;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type ENDRET_STARTDATO.
 * Lagrer ny og forrige startdato for oppgaven.
 */
@Entity(name = "EndretStartdatoOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_ENDRET_STARTDATO")
@Access(AccessType.FIELD)
public class EndretStartdatoOppgaveDataEntitet extends OppgaveDataEntitet {

    @Column(name = "ny_startdato", nullable = false, updatable = false)
    private LocalDate nyStartdato;

    @Column(name = "forrige_startdato", nullable = false, updatable = false)
    private LocalDate forrigeStartdato;

    protected EndretStartdatoOppgaveDataEntitet() {
        // For JPA
    }

    public EndretStartdatoOppgaveDataEntitet(LocalDate nyStartdato,
                                              LocalDate forrigeStartdato) {
        this.nyStartdato = nyStartdato;
        this.forrigeStartdato = forrigeStartdato;
    }


    public LocalDate getNyStartdato() {
        return nyStartdato;
    }

    public LocalDate getForrigeStartdato() {
        return forrigeStartdato;
    }
}
