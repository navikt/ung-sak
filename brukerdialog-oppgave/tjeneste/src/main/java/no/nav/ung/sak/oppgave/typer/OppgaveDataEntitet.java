package no.nav.ung.sak.oppgave.typer;

import jakarta.persistence.*;
import no.nav.ung.sak.BaseEntitet;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;

/**
 * Abstrakt superklasse for alle BD_OPPGAVE_DATA-tabeller.
 * Inneholder felles felter: fremmednøkkel til {@link BrukerdialogOppgaveEntitet}
 * samt audit-kolonner fra {@link BaseEntitet} (opprettet_av, opprettet_tid, endret_av, endret_tid).
 * Primærnøkkel og sekvens defineres i hver subklasse.
 */
@MappedSuperclass
public abstract class OppgaveDataEntitet extends BaseEntitet {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bd_oppgave_id", nullable = false, updatable = false)
    protected BrukerdialogOppgaveEntitet oppgave;

    protected OppgaveDataEntitet() {
        // For JPA
    }

    protected OppgaveDataEntitet(BrukerdialogOppgaveEntitet oppgave) {
        this.oppgave = oppgave;
    }

    public abstract Long getId();

    public BrukerdialogOppgaveEntitet getOppgave() {
        return oppgave;
    }
}

