package no.nav.ung.sak.oppgave.typer;

import jakarta.persistence.*;
import no.nav.ung.sak.BaseEntitet;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;

/**
 * Abstrakt entitet for alle BD_OPPGAVE_DATA-tabeller.
 * Bruker TABLE_PER_CLASS-arv slik at hver subklasse har sin egen tabell.
 * Hver subklasse har en FK (bd_oppgave_id) som peker tilbake på {@link no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet}.
 * Audit-kolonner arves fra {@link BaseEntitet} (opprettet_av, opprettet_tid, endret_av, endret_tid).
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Access(AccessType.FIELD)
public abstract class OppgaveDataEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA")
    protected Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bd_oppgave_id", nullable = false, updatable = false)
    private BrukerdialogOppgaveEntitet oppgave;

    protected OppgaveDataEntitet() {
        // For JPA
    }

    public Long getId() {
        return id;
    }

    public BrukerdialogOppgaveEntitet getOppgave() {
        return oppgave;
    }

    public void setOppgave(BrukerdialogOppgaveEntitet oppgave) {
        this.oppgave = oppgave;
    }
}
