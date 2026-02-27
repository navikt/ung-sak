package no.nav.ung.sak.oppgave.typer.varsel.typer.endretsluttdato;

import jakarta.persistence.*;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type ENDRET_SLUTTDATO.
 * Lagrer ny sluttdato og eventuell forrige sluttdato for oppgaven.
 */
@Entity(name = "EndretSluttdatoOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_ENDRET_SLUTTDATO")
@Access(AccessType.FIELD)
@OppgaveTypeRef(OppgaveType.BEKREFT_ENDRET_SLUTTDATO)
public class EndretSluttdatoOppgaveDataEntitet extends OppgaveDataEntitet {

    @Column(name = "ny_sluttdato", nullable = false, updatable = false)
    private LocalDate nySluttdato;

    /** Null dersom dette er første gang sluttdato settes. */
    @Column(name = "forrige_sluttdato", updatable = false)
    private LocalDate forrigeSluttdato;

    protected EndretSluttdatoOppgaveDataEntitet() {
        // For JPA
    }

    public EndretSluttdatoOppgaveDataEntitet(LocalDate nySluttdato,
                                              LocalDate forrigeSluttdato) {
        this.nySluttdato = nySluttdato;
        this.forrigeSluttdato = forrigeSluttdato;
    }


    public LocalDate getNySluttdato() {
        return nySluttdato;
    }

    public LocalDate getForrigeSluttdato() {
        return forrigeSluttdato;
    }
}
