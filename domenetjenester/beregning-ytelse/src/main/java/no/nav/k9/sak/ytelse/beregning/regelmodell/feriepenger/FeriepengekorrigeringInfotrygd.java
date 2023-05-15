package no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger;

import java.math.BigDecimal;
import java.time.Year;

import no.nav.k9.sak.typer.Arbeidsgiver;

public class FeriepengekorrigeringInfotrygd {

    private Year opptjeningsår;
    private boolean refusjon;
    private Arbeidsgiver arbeidsgiver;
    private BigDecimal korrigeringsbeløp;

    public static FeriepengekorrigeringInfotrygd forBruker(Year opptjeningsår, BigDecimal korrigeringsbeløp){
        return new FeriepengekorrigeringInfotrygd(opptjeningsår, false, null, korrigeringsbeløp);
    }

    public static FeriepengekorrigeringInfotrygd forRefusjon(Year opptjeningsår, Arbeidsgiver arbeidsgiver, BigDecimal korrigeringsbeløp){
        return new FeriepengekorrigeringInfotrygd(opptjeningsår, false, arbeidsgiver, korrigeringsbeløp);
    }

    private FeriepengekorrigeringInfotrygd(Year opptjeningsår, boolean refusjon, Arbeidsgiver arbeidsgiver, BigDecimal korrigeringsbeløp) {
        this.opptjeningsår = opptjeningsår;
        this.refusjon = refusjon;
        this.arbeidsgiver = arbeidsgiver;
        this.korrigeringsbeløp = korrigeringsbeløp;
    }

    public Year getOpptjeningsår() {
        return opptjeningsår;
    }

    public boolean erRefusjon() {
        return refusjon;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public BigDecimal getKorrigeringsbeløp() {
        return korrigeringsbeløp;
    }
}
