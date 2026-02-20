package no.nav.ung.sak.oppgave.typer.oppgave.søkytelse;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type SØK_YTELSE.
 * Lagrer fra-og-med-dato for når bruker kan søke ytelsen.
 */
@Entity(name = "SøkYtelseOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_SOK_YTELSE")
public class SøkYtelseOppgaveDataEntitet extends OppgaveDataEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_SOK_YTELSE")
    @SequenceGenerator(name = "SEQ_BD_OPPGAVE_DATA_SOK_YTELSE", sequenceName = "SEQ_BD_OPPGAVE_DATA_SOK_YTELSE", allocationSize = 1)
    protected Long id;

    @Column(name = "fom_dato", nullable = false, updatable = false)
    private LocalDate fomDato;

    protected SøkYtelseOppgaveDataEntitet() {
        // For JPA
    }

    public SøkYtelseOppgaveDataEntitet(BrukerdialogOppgaveEntitet oppgave, LocalDate fomDato) {
        super(oppgave);
        this.fomDato = fomDato;
    }

    @Override
    public Long getId() {
        return id;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }
}
