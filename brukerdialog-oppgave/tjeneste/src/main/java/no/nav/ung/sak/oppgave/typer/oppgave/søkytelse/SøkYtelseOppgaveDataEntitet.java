package no.nav.ung.sak.oppgave.typer.oppgave.søkytelse;

import jakarta.persistence.*;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;

import java.time.LocalDate;

/**
 * Databasestruktur for oppgavedata av type SØK_YTELSE.
 * Lagrer fra-og-med-dato for når bruker kan søke ytelsen.
 */
@Entity(name = "SøkYtelseOppgaveData")
@Table(name = "BD_OPPGAVE_DATA_SOK_YTELSE")
public class SøkYtelseOppgaveDataEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_SOK_YTELSE")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bd_oppgave_id", nullable = false, updatable = false)
    private BrukerdialogOppgaveEntitet oppgave;

    @Column(name = "fom_dato", nullable = false, updatable = false)
    private LocalDate fomDato;

    protected SøkYtelseOppgaveDataEntitet() {
        // For JPA
    }

    public SøkYtelseOppgaveDataEntitet(BrukerdialogOppgaveEntitet oppgave, LocalDate fomDato) {
        this.oppgave = oppgave;
        this.fomDato = fomDato;
    }

    public Long getId() {
        return id;
    }

    public BrukerdialogOppgaveEntitet getOppgave() {
        return oppgave;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }
}

