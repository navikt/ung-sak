package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.persistence.*;
import no.nav.ung.sak.diff.DiffIgnore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.Objects;

@Entity(name = "Beregningsgrunnlag")
@Table(name = "BEREGNINGSGRUNNLAG")
public class Beregningsgrunnlag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEREGNINGSGRUNNLAG")
    private Long id;

    @Column(name = "skjaeringstidspunkt", nullable = false, updatable = false)
    private LocalDate skjæringstidspunkt;

    @Column(name = "siste_lignede_aar", nullable = false, updatable = false)
    private Year sisteLignedeÅr;

    @Column(name = "aarsinntekt_siste_aar", nullable = false, updatable = false)
    private BigDecimal årsinntektAvkortetOppjustertSisteÅr;

    @Column(name = "aarsinntekt_siste_tre_aar", nullable = false, updatable = false)
    private BigDecimal årsinntektAvkortetOppjustertSisteTreÅr;

    @Column(name = "beregnet_pr_aar", nullable = false, updatable = false)
    private BigDecimal beregnetPrAar;

    @Column(name = "beregnet_redusert_pr_aar", nullable = false, updatable = false)
    private BigDecimal beregnetRedusertPrAar;

    @Embedded
    private BeregningsgrunnlagInput beregningInput;

    @Lob
    @DiffIgnore
    @Column(name = "regel_sporing", nullable = false, updatable = false, length = 100000)
    private String regelSporing;

    protected Beregningsgrunnlag() {
    }

    public Beregningsgrunnlag(BeregningInput beregningInputGrunnlag, BigDecimal årsinntektSisteÅr, BigDecimal årsinntektSisteTreÅr, BigDecimal beregnetPrAar, BigDecimal beregnetRedusertPrAar, String regelSporing) {
        this.skjæringstidspunkt = beregningInputGrunnlag.skjæringstidspunkt();
        this.sisteLignedeÅr = beregningInputGrunnlag.sisteLignedeÅr();
        this.årsinntektAvkortetOppjustertSisteÅr = årsinntektSisteÅr;
        this.årsinntektAvkortetOppjustertSisteTreÅr = årsinntektSisteTreÅr;
        this.beregnetPrAar = beregnetPrAar;
        this.beregnetRedusertPrAar = beregnetRedusertPrAar;
        this.regelSporing = regelSporing;
        this.beregningInput = new BeregningsgrunnlagInput(beregningInputGrunnlag);
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public BigDecimal getÅrsinntektAvkortetOppjustertSisteÅr() {
        return årsinntektAvkortetOppjustertSisteÅr;
    }

    public BigDecimal getÅrsinntektAvkortetOppjustertSisteTreÅr() {
        return årsinntektAvkortetOppjustertSisteTreÅr;
    }

    public BigDecimal getBeregnetPrAar() {
        return beregnetPrAar;
    }

    public BigDecimal getBeregnetRedusertPrAar() {
        return beregnetRedusertPrAar;
    }

    public BigDecimal getDagsats() {
        return beregnetRedusertPrAar.divide(BigDecimal.valueOf(260), 10, RoundingMode.HALF_UP);
    }

    public BesteBeregningResultatType utledBesteBeregningResultatType() {
        if (beregnetPrAar.compareTo(årsinntektAvkortetOppjustertSisteÅr) == 0) {
            return BesteBeregningResultatType.SISTE_ÅR;
        }
        return BesteBeregningResultatType.SNITT_SISTE_TRE_ÅR;
    }

    public BeregningsgrunnlagInput getBeregningInput() {
        return beregningInput;
    }

    public String getRegelSporing() {
        return regelSporing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Beregningsgrunnlag that)) return false;
        return Objects.equals(getSkjæringstidspunkt(), that.getSkjæringstidspunkt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSkjæringstidspunkt());
    }

    public Year getSisteLignedeÅr() {
        return sisteLignedeÅr;
    }
}
