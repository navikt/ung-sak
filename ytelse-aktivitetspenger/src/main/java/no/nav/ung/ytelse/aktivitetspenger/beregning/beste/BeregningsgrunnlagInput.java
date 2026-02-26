package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

@Embeddable
public class BeregningsgrunnlagInput {

    @Column(name = "pgi1_aar", nullable = false, updatable = false)
    private int pgi1År;

    @Column(name = "pgi1_aarsinntekt", nullable = false, updatable = false)
    private BigDecimal pgi1Årsinntekt;

    @Column(name = "pgi2_aar", nullable = false, updatable = false)
    private int pgi2År;

    @Column(name = "pgi2_aarsinntekt", nullable = false, updatable = false)
    private BigDecimal pgi2Årsinntekt;

    @Column(name = "pgi3_aar", nullable = false, updatable = false)
    private int pgi3År;

    @Column(name = "pgi3_aarsinntekt", nullable = false, updatable = false)
    private BigDecimal pgi3Årsinntekt;

    protected BeregningsgrunnlagInput() {
    }

    public BeregningsgrunnlagInput(BeregningInput beregningInput) {
        var sisteLignedeÅr = beregningInput.sisteLignedeÅr();

        this.pgi1År = sisteLignedeÅr.minusYears(2).getValue();
        this.pgi1Årsinntekt = beregningInput.pgi1().getVerdi();

        this.pgi2År = sisteLignedeÅr.minusYears(1).getValue();
        this.pgi2Årsinntekt = beregningInput.pgi2().getVerdi();

        this.pgi3År = sisteLignedeÅr.getValue();
        this.pgi3Årsinntekt = beregningInput.pgi3().getVerdi();
    }

    public BeregningInput getBeregningInput(LocalDate virkningsdato) {
        return new BeregningInput(
            new Beløp(pgi1Årsinntekt),
            new Beløp(pgi2Årsinntekt),
            new Beløp(pgi3Årsinntekt),
            virkningsdato,
            Year.of(pgi3År)
        );
    }
}
