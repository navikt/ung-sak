package no.nav.ung.sak.oppgave.typer.oppgave.søkytelse;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type SØK_YTELSE.
 * Lagrer fra-og-med-dato for når bruker kan søke ytelsen.
 */
@Entity(name = "SøkYtelseOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_SOK_YTELSE")
public class SøkYtelseOppgaveDataEntitet extends OppgaveDataEntitet {

    @Column(name = "fom_dato", nullable = false, updatable = false)
    private LocalDate fomDato;

    protected SøkYtelseOppgaveDataEntitet() {
        // For JPA
    }

    public SøkYtelseOppgaveDataEntitet(LocalDate fomDato) {
        this.fomDato = fomDato;
    }


    public LocalDate getFomDato() {
        return fomDato;
    }
}
