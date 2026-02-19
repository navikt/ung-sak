package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import java.math.BigDecimal;

public class BesteBeregningResultat {

    private final BigDecimal sisteÅrVerdi;
    private final BigDecimal snittTreSisteÅr;
    private final BigDecimal besteBeregning;
    private final String regelSporing;
    private final String regelInput;

    public BesteBeregningResultat(BigDecimal sisteÅrVerdi, BigDecimal snittTreSisteÅr, BigDecimal besteBeregning, String regelSporing, String regelInput) {
        this.sisteÅrVerdi = sisteÅrVerdi;
        this.snittTreSisteÅr = snittTreSisteÅr;
        this.besteBeregning = besteBeregning;
        this.regelSporing = regelSporing;
        this.regelInput = regelInput;
    }

    public BigDecimal getSisteÅrVerdi() {
        return sisteÅrVerdi;
    }

    public BigDecimal getSnittTreSisteÅr() {
        return snittTreSisteÅr;
    }

    public BigDecimal getBesteBeregning() {
        return besteBeregning;
    }

    public String getRegelSporing() {
        return regelSporing;
    }

    public String getRegelInput() {
        return regelInput;
    }
}
