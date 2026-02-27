package no.nav.ung.sak.oppgave.typer.varsel.typer.kontrollerregisterinntekt;

import jakarta.persistence.*;
import no.nav.ung.sak.BaseEntitet;

/**
 * En enkeltpost for arbeid- eller frilansinntekt knyttet til en
 * {@link KontrollerRegisterinntektOppgaveDataEntitet}.
 */
@Entity(name = "ArbeidOgFrilansInntekt")
@Table(name = "BD_OPPGAVE_DATA_ARBEID_FRILANS_INNTEKT")
@Access(AccessType.FIELD)
public class ArbeidOgFrilansInntektEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_ARBEID_FRILANS_INNTEKT")
    private Long id;

    @Column(name = "arbeidsgiver", nullable = false, updatable = false)
    private String arbeidsgiver;

    @Column(name = "inntekt", nullable = false, updatable = false)
    private int inntekt;

    protected ArbeidOgFrilansInntektEntitet() {
        // For JPA
    }

    ArbeidOgFrilansInntektEntitet(String arbeidsgiver, int inntekt) {
        this.arbeidsgiver = arbeidsgiver;
        this.inntekt = inntekt;
    }

    public Long getId() {
        return id;
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public int getInntekt() {
        return inntekt;
    }
}
