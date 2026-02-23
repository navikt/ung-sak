package no.nav.ung.sak.oppgave.typer.varsel.typer.kontrollerregisterinntekt;

import jakarta.persistence.*;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.YtelseType;

/**
 * En enkeltpost for ytelseinntekt knyttet til en
 * {@link KontrollerRegisterinntektOppgaveDataEntitet}.
 */
@Entity(name = "YtelseInntekt")
@Table(name = "BD_OPPGAVE_DATA_YTELSE_INNTEKT")
@SequenceGenerator(name = "SEQ_BD_OPPGAVE_DATA_YTELSE_INNTEKT", sequenceName = "SEQ_BD_OPPGAVE_DATA_YTELSE_INNTEKT", allocationSize = 50)
public class YtelseInntektEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE_DATA_YTELSE_INNTEKT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kontroller_data_id", nullable = false, updatable = false)
    private KontrollerRegisterinntektOppgaveDataEntitet oppgaveData;

    @Column(name = "ytelsetype", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private YtelseType ytelsetype;

    @Column(name = "inntekt", nullable = false, updatable = false)
    private int inntekt;

    protected YtelseInntektEntitet() {
        // For JPA
    }

    public YtelseInntektEntitet(KontrollerRegisterinntektOppgaveDataEntitet oppgaveData,
                                 YtelseType ytelsetype,
                                 int inntekt) {
        this.oppgaveData = oppgaveData;
        this.ytelsetype = ytelsetype;
        this.inntekt = inntekt;
    }

    public Long getId() {
        return id;
    }

    public KontrollerRegisterinntektOppgaveDataEntitet getOppgaveData() {
        return oppgaveData;
    }

    public YtelseType getYtelsetype() {
        return ytelsetype;
    }

    public int getInntekt() {
        return inntekt;
    }
}

