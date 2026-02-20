package no.nav.ung.sak.oppgave.typer.varsel.typer.kontrollerregisterinntekt;

import jakarta.persistence.*;

/**
 * En enkeltpost for arbeid- eller frilansinntekt knyttet til en
 * {@link KontrollerRegisterinntektOppgaveDataEntitet}.
 */
@Entity(name = "ArbeidOgFrilansInntekt")
@Table(name = "BD_OPPGAVE_DATA_ARBEID_FRILANS_INNTEKT")
public class ArbeidOgFrilansInntektEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kontroller_data_id", nullable = false, updatable = false)
    private KontrollerRegisterinntektOppgaveDataEntitet oppgaveData;

    @Column(name = "arbeidsgiver", nullable = false, updatable = false)
    private String arbeidsgiver;

    @Column(name = "inntekt", nullable = false, updatable = false)
    private int inntekt;

    protected ArbeidOgFrilansInntektEntitet() {
        // For JPA
    }

    public ArbeidOgFrilansInntektEntitet(KontrollerRegisterinntektOppgaveDataEntitet oppgaveData,
                                          String arbeidsgiver,
                                          int inntekt) {
        this.oppgaveData = oppgaveData;
        this.arbeidsgiver = arbeidsgiver;
        this.inntekt = inntekt;
    }

    public Long getId() {
        return id;
    }

    public KontrollerRegisterinntektOppgaveDataEntitet getOppgaveData() {
        return oppgaveData;
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public int getInntekt() {
        return inntekt;
    }
}

