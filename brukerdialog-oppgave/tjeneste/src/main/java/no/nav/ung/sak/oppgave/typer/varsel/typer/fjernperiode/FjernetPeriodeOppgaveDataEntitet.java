package no.nav.ung.sak.oppgave.typer.varsel.typer.fjernperiode;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type FJERNET_PERIODE.
 * Lagrer forrige startdato og eventuell sluttdato for perioden som ble fjernet.
 */
@Entity(name = "FjernetPeriodeOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_FJERNET_PERIODE")
public class FjernetPeriodeOppgaveDataEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_FJERNET_PERIODE")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bd_oppgave_id", nullable = false, updatable = false)
    private BrukerdialogOppgaveEntitet oppgave;

    @Column(name = "forrige_startdato", nullable = false, updatable = false)
    private LocalDate forrigeStartdato;

    /** Null dersom perioden var åpen (ingen sluttdato). */
    @Column(name = "forrige_sluttdato", updatable = false)
    private LocalDate forrigeSluttdato;

    protected FjernetPeriodeOppgaveDataEntitet() {
        // For JPA
    }

    public FjernetPeriodeOppgaveDataEntitet(BrukerdialogOppgaveEntitet oppgave,
                                             LocalDate forrigeStartdato,
                                             LocalDate forrigeSluttdato) {
        this.oppgave = oppgave;
        this.forrigeStartdato = forrigeStartdato;
        this.forrigeSluttdato = forrigeSluttdato;
    }

    public Long getId() {
        return id;
    }

    public BrukerdialogOppgaveEntitet getOppgave() {
        return oppgave;
    }

    public LocalDate getForrigeStartdato() {
        return forrigeStartdato;
    }

    public LocalDate getForrigeSluttdato() {
        return forrigeSluttdato;
    }
}

