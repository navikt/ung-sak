package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.persistence.*;
import no.nav.ung.sak.diff.DiffIgnore;

import java.math.BigDecimal;
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

    @Column(name = "beregningsgrunnlag", nullable = false, updatable = false)
    private BigDecimal beregningsgrunnlag;

    @Column(name = "beregningsgrunnlag_redusert", nullable = false, updatable = false)
    private BigDecimal beregningsgrunnlagRedusert;

    @Column(name = "dagsats", nullable = false, updatable = false)
    private BigDecimal dagsats;

    @Embedded
    private BeregningsgrunnlagInput beregningInput;

    @Lob
    @DiffIgnore
    @Column(name = "regel_sporing", nullable = false, updatable = false, length = 100000)
    private String regelSporing;

    protected Beregningsgrunnlag() {
    }

    public Beregningsgrunnlag(BeregningInput beregningInputGrunnlag, BigDecimal årsinntektSisteÅr, BigDecimal årsinntektSisteTreÅr, BigDecimal beregningsgrunnlag, BigDecimal beregningsgrunnlagRedusert, BigDecimal dagsats, String regelSporing) {
        this.skjæringstidspunkt = beregningInputGrunnlag.virkningsdato();
        this.sisteLignedeÅr = beregningInputGrunnlag.sisteLignedeÅr();
        this.årsinntektAvkortetOppjustertSisteÅr = årsinntektSisteÅr;
        this.årsinntektAvkortetOppjustertSisteTreÅr = årsinntektSisteTreÅr;
        this.beregningsgrunnlag = beregningsgrunnlag;
        this.beregningsgrunnlagRedusert = beregningsgrunnlagRedusert;
        this.dagsats = dagsats;
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

    public BigDecimal getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    public BigDecimal getBeregningsgrunnlagRedusert() {
        return beregningsgrunnlagRedusert;
    }

    public BigDecimal getDagsats() {
        return dagsats;
    }

    public BesteBeregningResultatType utledBesteBeregningResultatType() {
        if (beregningsgrunnlag.compareTo(årsinntektAvkortetOppjustertSisteÅr) == 0) {
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
}
