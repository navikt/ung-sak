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
    private BigDecimal inntektGraderingsprosent;
    private BigDecimal graderingsfaktorTid;
    private BigDecimal graderingsfaktorInntekt;


    public BeregningsresultatPeriode(LocalDateInterval periode,
                                     BigDecimal inntektGraderingsprosent,
                                     BigDecimal graderingsfaktorTid,
                                     BigDecimal graderingsfaktorInntekt) {
        this.periode = periode;
        this.inntektGraderingsprosent = inntektGraderingsprosent;
        this.graderingsfaktorTid = graderingsfaktorTid;
        this.graderingsfaktorInntekt = graderingsfaktorInntekt;
    }

    public BeregningsresultatPeriode(LocalDate fom,
                                     LocalDate tom,
                                     BigDecimal inntektGraderingsprosent,
                                     BigDecimal graderingsfaktorTid,
                                     BigDecimal graderingsfaktorInntekt) {
        this(new LocalDateInterval(fom, tom), inntektGraderingsprosent, graderingsfaktorTid, graderingsfaktorInntekt);
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

    public BigDecimal getInntektGraderingsprosent() {
        return inntektGraderingsprosent;
    }

    public BigDecimal getGraderingsfaktorTid() {
        return graderingsfaktorTid;
    }

    public BigDecimal getGraderingsfaktorInntekt() {
        return graderingsfaktorInntekt;
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
