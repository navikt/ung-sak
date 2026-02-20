package no.nav.ung.sak.oppgave.typer.varsel.typer.endretstartdato;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type ENDRET_STARTDATO.
 * Lagrer ny og forrige startdato for oppgaven.
 */
@Entity(name = "EndretStartdatoOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_ENDRET_STARTDATO")
public class EndretStartdatoOppgaveDataEntitet extends OppgaveDataEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_ENDRET_STARTDATO")
    @SequenceGenerator(name = "SEQ_BD_OPPGAVE_DATA_ENDRET_STARTDATO", sequenceName = "SEQ_BD_OPPGAVE_DATA_ENDRET_STARTDATO", allocationSize = 1)
    protected Long id;

    @Column(name = "ny_startdato", nullable = false, updatable = false)
    private LocalDate nyStartdato;

    @Column(name = "forrige_startdato", nullable = false, updatable = false)
    private LocalDate forrigeStartdato;

    protected EndretStartdatoOppgaveDataEntitet() {
        // For JPA
    }

    public EndretStartdatoOppgaveDataEntitet(BrukerdialogOppgaveEntitet oppgave,
                                              LocalDate nyStartdato,
                                              LocalDate forrigeStartdato) {
        super(oppgave);
        this.nyStartdato = nyStartdato;
        this.forrigeStartdato = forrigeStartdato;
    }

    @Override
    public Long getId() {
        return id;
    }

    public LocalDate getNyStartdato() {
        return nyStartdato;
    }

    public LocalDate getForrigeStartdato() {
        return forrigeStartdato;
    }
}
