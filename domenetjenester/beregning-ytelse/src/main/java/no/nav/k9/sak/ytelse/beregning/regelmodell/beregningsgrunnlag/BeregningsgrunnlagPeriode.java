package no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonManagedReference;

public class BeregningsgrunnlagPeriode {
    @JsonManagedReference
    private List<BeregningsgrunnlagPrStatus> beregningsgrunnlagPrStatus = new ArrayList<>();
    private Periode bgPeriode;
    private BigDecimal totalUtbetalingsgradFraUttak;
    private BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
    private BigDecimal reduksjonsfaktorInaktivTypeA;
    private BigDecimal bruttoBeregningsgrunnlag;


    public BeregningsgrunnlagPeriode() {
    }

    public List<BeregningsgrunnlagPrStatus> getBeregningsgrunnlagPrStatus(AktivitetStatus aktivitetStatus) {
        return beregningsgrunnlagPrStatus.stream()
            .filter(af -> aktivitetStatus.equals(af.getAktivitetStatus()))
            .collect(Collectors.toList());
    }

    public List<BeregningsgrunnlagPrStatus> getBeregningsgrunnlagPrStatus() {
        return beregningsgrunnlagPrStatus;
    }

    void addBeregningsgrunnlagPrStatus(BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus) {
        Objects.requireNonNull(beregningsgrunnlagPrStatus, "beregningsgrunnlagPrStatus");
        Objects.requireNonNull(beregningsgrunnlagPrStatus.getAktivitetStatus(), "aktivitetStatus");
        this.beregningsgrunnlagPrStatus.add(beregningsgrunnlagPrStatus);
    }

    public Periode getBeregningsgrunnlagPeriode() {
        return bgPeriode;
    }

    public BigDecimal getTotalUtbetalingsgradFraUttak() {
        return totalUtbetalingsgradFraUttak;
    }

    public BigDecimal getTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt() {
        return totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
    }

    public BigDecimal getReduksjonsfaktorInaktivTypeA() {
        return reduksjonsfaktorInaktivTypeA;
    }

    public static Builder builder() {
        return new Builder();
    }

    public BigDecimal getBruttoBeregningsgrunnlag() {
        return bruttoBeregningsgrunnlag;
    }

    public static class Builder {
        private BeregningsgrunnlagPeriode beregningsgrunnlagPeriodeMal;

        public Builder() {
            beregningsgrunnlagPeriodeMal = new BeregningsgrunnlagPeriode();
        }

        public Builder medPeriode(Periode beregningsgrunnlagPeriode) {
            beregningsgrunnlagPeriodeMal.bgPeriode = beregningsgrunnlagPeriode;
            return this;
        }

        public Builder medTotalUtbetalingsgradFraUttak(BigDecimal totalUtbetalingsgradFraUttak) {
            beregningsgrunnlagPeriodeMal.totalUtbetalingsgradFraUttak = totalUtbetalingsgradFraUttak;
            return this;
        }

        public Builder medTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt(BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt) {
            beregningsgrunnlagPeriodeMal.totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt = totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
            return this;
        }

        public Builder medReduksjonsfaktorInaktivTypeA(BigDecimal reduksjonsfaktorInaktivTypeA) {
            beregningsgrunnlagPeriodeMal.reduksjonsfaktorInaktivTypeA = reduksjonsfaktorInaktivTypeA;
            return this;
        }

        public Builder medBeregningsgrunnlagPrStatus(BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus) {
            beregningsgrunnlagPeriodeMal.addBeregningsgrunnlagPrStatus(beregningsgrunnlagPrStatus);
            return this;
        }

        public Builder medBruttoBeregningsgrunnlag(BigDecimal bruttoBeregningsgrunnlag) {
            beregningsgrunnlagPeriodeMal.bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlag;
            return this;
        }

        public BeregningsgrunnlagPeriode build() {
            verifyStateForBuild();
            return beregningsgrunnlagPeriodeMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(beregningsgrunnlagPeriodeMal.beregningsgrunnlagPrStatus, "beregningsgrunnlagPrStatus");
            Objects.requireNonNull(beregningsgrunnlagPeriodeMal.bgPeriode, "bgPeriode");
            Objects.requireNonNull(beregningsgrunnlagPeriodeMal.bgPeriode.getFom(), "bgPeriode.getFom()");
            Objects.requireNonNull(beregningsgrunnlagPeriodeMal.bruttoBeregningsgrunnlag, "bruttoBeregningsgrunnlag");
        }
    }
}
