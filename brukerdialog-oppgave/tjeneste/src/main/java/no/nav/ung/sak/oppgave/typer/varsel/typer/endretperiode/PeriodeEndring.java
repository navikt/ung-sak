package no.nav.ung.sak.oppgave.typer.varsel.typer.endretperiode;

import jakarta.persistence.*;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType;

/**
 * Representerer én enkelt endring knyttet til en {@link EndretPeriodeOppgaveDataEntitet}.
 */
@Entity(name = "PeriodeEndring")
@Table(name = "BD_OPPGAVE_DATA_PERIODE_ENDRING")
@SequenceGenerator(name = "SEQ_BD_OPPGAVE_DATA_PERIODE_ENDRING", sequenceName = "SEQ_BD_OPPGAVE_DATA_PERIODE_ENDRING", allocationSize = 50)
public class PeriodeEndring {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_PERIODE_ENDRING")
    private Long id;

    @Column(name = "endring_type", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private PeriodeEndringType endringType;

    protected PeriodeEndring() {
        // For JPA
    }

    PeriodeEndring(PeriodeEndringType endringType) {
        this.endringType = endringType;
    }

    public Long getId() {
        return id;
    }

    public PeriodeEndringType getEndringType() {
        return endringType;
    }
}

