package no.nav.k9.sak.ytelse.beregning.regelmodell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public class BeregningsresultatPeriode {

    private List<BeregningsresultatAndel> beregningsresultatAndelList = new ArrayList<>();
    private LocalDateInterval periode;
    private BigDecimal totalUtbetalingsgradFraUttak;
    private BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
    private BigDecimal reduksjonsfaktorInaktivTypeA;

    public BeregningsresultatPeriode(LocalDateInterval periode,
                                     BigDecimal totalUtbetalingsgradFraUttak,
                                     BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt,
                                     BigDecimal reduksjonsfaktorInaktivTypeA) {
        this.periode = periode;
        this.totalUtbetalingsgradFraUttak = totalUtbetalingsgradFraUttak;
        this.totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt = totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt;
        this.reduksjonsfaktorInaktivTypeA = reduksjonsfaktorInaktivTypeA;
    }

    public BeregningsresultatPeriode(LocalDate fom,
                                     LocalDate tom,
                                     BigDecimal totalUtbetalingsgradFraUttak,
                                     BigDecimal totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt,
                                     BigDecimal reduksjonsfaktorInaktivTypeA) {
        this(new LocalDateInterval(fom, tom), totalUtbetalingsgradFraUttak, totalUtbetalingsgradEtterReduksjonVedTilkommetInntekt, reduksjonsfaktorInaktivTypeA);
    }

    public LocalDate getFom() {
        return periode.getFomDato();
    }

    public LocalDate getTom() {
        return periode.getTomDato();
    }

    public LocalDateInterval getPeriode() {
        return periode;
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

    public List<BeregningsresultatAndel> getBeregningsresultatAndelList() {
        return beregningsresultatAndelList;
    }

    public boolean inneholder(LocalDate dato) {
        return periode.encloses(dato);
    }

    public void addBeregningsresultatAndel(BeregningsresultatAndel andel) {
        Objects.requireNonNull(andel, "beregningsresultatAndel");
        if (!beregningsresultatAndelList.contains(andel)) {
            beregningsresultatAndelList.add(andel);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + periode + ", andeler=" + beregningsresultatAndelList + ">";
    }

}
