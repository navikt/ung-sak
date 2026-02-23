package no.nav.ung.sak.oppgave.typer;

import jakarta.persistence.*;
import no.nav.ung.sak.BaseEntitet;

/**
 * Abstrakt entitet for alle BD_OPPGAVE_DATA-tabeller.
 * Bruker TABLE_PER_CLASS-arv slik at hver subklasse har sin egen tabell,
 * og {@link no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet} kan ha en {@code @Any}-referanse hit.
 * Audit-kolonner arves fra {@link BaseEntitet} (opprettet_av, opprettet_tid, endret_av, endret_tid).
 * Primærnøkkel og sekvens defineres i hver subklasse.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@SequenceGenerator(name = "SEQ_BD_OPPGAVE_DATA", sequenceName = "SEQ_BD_OPPGAVE_DATA", allocationSize = 50)
public abstract class OppgaveDataEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA")
    protected Long id;

    protected OppgaveDataEntitet() {
        // For JPA
    }

    public Long getId() {
        return id;
    }
}
