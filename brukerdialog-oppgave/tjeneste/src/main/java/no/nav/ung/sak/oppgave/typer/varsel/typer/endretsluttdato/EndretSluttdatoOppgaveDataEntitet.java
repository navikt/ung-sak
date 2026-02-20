package no.nav.ung.sak.oppgave.typer.varsel.typer.endretsluttdato;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type ENDRET_SLUTTDATO.
 * Lagrer ny sluttdato og eventuell forrige sluttdato for oppgaven.
 */
@Entity(name = "EndretSluttdatoOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_ENDRET_SLUTTDATO")
public class EndretSluttdatoOppgaveDataEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_ENDRET_SLUTTDATO")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bd_oppgave_id", nullable = false, updatable = false)
    private BrukerdialogOppgaveEntitet oppgave;

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
        this.oppgave = oppgave;
        this.nySluttdato = nySluttdato;
        this.forrigeSluttdato = forrigeSluttdato;
    }

    public Long getId() {
        return id;
    }

    public BrukerdialogOppgaveEntitet getOppgave() {
        return oppgave;
    }

    public LocalDate getNySluttdato() {
        return nySluttdato;
    }

    public LocalDate getForrigeSluttdato() {
        return forrigeSluttdato;
    }
}

