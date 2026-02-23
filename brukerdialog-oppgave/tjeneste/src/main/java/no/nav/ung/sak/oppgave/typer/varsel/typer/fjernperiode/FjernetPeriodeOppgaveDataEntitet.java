package no.nav.ung.sak.oppgave.typer.varsel.typer.fjernperiode;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type FJERNET_PERIODE.
 * Lagrer forrige startdato og eventuell sluttdato for perioden som ble fjernet.
 */
@Entity(name = "FjernetPeriodeOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_FJERNET_PERIODE")
@Access(AccessType.FIELD)
public class FjernetPeriodeOppgaveDataEntitet extends OppgaveDataEntitet {

    @Column(name = "forrige_startdato", nullable = false, updatable = false)
    private LocalDate forrigeStartdato;

    /** Null dersom perioden var åpen (ingen sluttdato). */
    @Column(name = "forrige_sluttdato", updatable = false)
    private LocalDate forrigeSluttdato;

    protected FjernetPeriodeOppgaveDataEntitet() {
        // For JPA
    }

    public FjernetPeriodeOppgaveDataEntitet(LocalDate forrigeStartdato,
                                             LocalDate forrigeSluttdato) {
        this.forrigeStartdato = forrigeStartdato;
        this.forrigeSluttdato = forrigeSluttdato;
    }


    public LocalDate getForrigeStartdato() {
        return forrigeStartdato;
    }

    public LocalDate getForrigeSluttdato() {
        return forrigeSluttdato;
    }
}
