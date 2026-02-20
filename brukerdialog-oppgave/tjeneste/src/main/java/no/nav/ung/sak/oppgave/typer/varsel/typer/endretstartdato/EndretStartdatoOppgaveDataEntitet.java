package no.nav.ung.sak.oppgave.typer.varsel.typer.endretstartdato;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type ENDRET_STARTDATO.
 * Lagrer ny og forrige startdato for oppgaven.
 */
@Entity(name = "EndretStartdatoOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_ENDRET_STARTDATO")
public class EndretStartdatoOppgaveDataEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_ENDRET_STARTDATO")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bd_oppgave_id", nullable = false, updatable = false)
    private BrukerdialogOppgaveEntitet oppgave;

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
        this.oppgave = oppgave;
        this.nyStartdato = nyStartdato;
        this.forrigeStartdato = forrigeStartdato;
    }

    public Long getId() {
        return id;
    }

    public BrukerdialogOppgaveEntitet getOppgave() {
        return oppgave;
    }

    public LocalDate getNyStartdato() {
        return nyStartdato;
    }

    public LocalDate getForrigeStartdato() {
        return forrigeStartdato;
    }
}


