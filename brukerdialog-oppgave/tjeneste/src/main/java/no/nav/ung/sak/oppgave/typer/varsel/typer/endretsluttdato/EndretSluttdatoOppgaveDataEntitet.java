package no.nav.ung.sak.oppgave.typer.varsel.typer.endretsluttdato;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type ENDRET_SLUTTDATO.
 * Lagrer ny sluttdato og eventuell forrige sluttdato for oppgaven.
 */
@Entity(name = "EndretSluttdatoOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_ENDRET_SLUTTDATO")
public class EndretSluttdatoOppgaveDataEntitet extends OppgaveDataEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_ENDRET_SLUTTDATO")
    @SequenceGenerator(name = "SEQ_BD_OPPGAVE_DATA_ENDRET_SLUTTDATO", sequenceName = "SEQ_BD_OPPGAVE_DATA_ENDRET_SLUTTDATO", allocationSize = 1)
    protected Long id;

    @Column(name = "ny_sluttdato", nullable = false, updatable = false)
    private LocalDate nySluttdato;

    /** Null dersom dette er første gang sluttdato settes. */
    @Column(name = "forrige_sluttdato", updatable = false)
    private LocalDate forrigeSluttdato;

    protected EndretSluttdatoOppgaveDataEntitet() {
        // For JPA
    }

    public EndretSluttdatoOppgaveDataEntitet(BrukerdialogOppgaveEntitet oppgave,
                                              LocalDate nySluttdato,
                                              LocalDate forrigeSluttdato) {
        super(oppgave);
        this.nySluttdato = nySluttdato;
        this.forrigeSluttdato = forrigeSluttdato;
    }

    @Override
    public Long getId() {
        return id;
    }

    public LocalDate getNySluttdato() {
        return nySluttdato;
    }

    public LocalDate getForrigeSluttdato() {
        return forrigeSluttdato;
    }
}
