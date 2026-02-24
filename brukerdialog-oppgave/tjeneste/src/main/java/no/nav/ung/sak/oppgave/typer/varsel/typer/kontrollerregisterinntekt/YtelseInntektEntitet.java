package no.nav.ung.sak.oppgave.typer.varsel.typer.kontrollerregisterinntekt;

import jakarta.persistence.*;
import no.nav.ung.sak.BaseEntitet;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType;

/**
 * En enkeltpost for ytelseinntekt knyttet til en
 * {@link KontrollerRegisterinntektOppgaveDataEntitet}.
 */
@Entity(name = "YtelseInntekt")
@Table(name = "BD_OPPGAVE_DATA_YTELSE_INNTEKT")
@Access(AccessType.FIELD)
public class YtelseInntektEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_YTELSE_INNTEKT")
    private Long id;

    @Column(name = "ytelsetype", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private YtelseType ytelsetype;

    @Column(name = "inntekt", nullable = false, updatable = false)
    private int inntekt;

    protected YtelseInntektEntitet() {
        // For JPA
    }

    YtelseInntektEntitet(YtelseType ytelsetype, int inntekt) {
        this.ytelsetype = ytelsetype;
        this.inntekt = inntekt;
    }

    public Long getId() {
        return id;
    }

    public YtelseType getYtelsetype() {
        return ytelsetype;
    }

    public int getInntekt() {
        return inntekt;
    }
}
