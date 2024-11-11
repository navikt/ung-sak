package no.nav.ung.sak.domene.iay.modell;

import java.math.BigDecimal;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Beløp;
import no.nav.ung.sak.typer.Stillingsprosent;

public class YtelseAnvistBuilder {
    private final YtelseAnvist ytelseAnvist;

    YtelseAnvistBuilder(YtelseAnvist ytelseAnvist) {
        this.ytelseAnvist = ytelseAnvist;
    }

    static YtelseAnvistBuilder ny() {
        return new YtelseAnvistBuilder(new YtelseAnvist());
    }

    public YtelseAnvistBuilder medBeløp(BigDecimal beløp) {
        if (beløp != null) {
            this.ytelseAnvist.setBeløp(new Beløp(beløp));
        }
        return this;
    }

    public YtelseAnvistBuilder medDagsats(BigDecimal dagsats) {
        if (dagsats != null) {
            this.ytelseAnvist.setDagsats(new Beløp(dagsats));
        }
        return this;
    }

    public YtelseAnvistBuilder medAnvistPeriode(DatoIntervallEntitet intervallEntitet){
        this.ytelseAnvist.setAnvistPeriode(intervallEntitet);
        return this;
    }

    public YtelseAnvistBuilder medUtbetalingsgradProsent(BigDecimal utbetalingsgradProsent) {
        if (utbetalingsgradProsent != null) {
            this.ytelseAnvist.setUtbetalingsgradProsent(new Stillingsprosent(utbetalingsgradProsent));
        }
        return this;
    }

    public YtelseAnvistBuilder leggTilYtelseAnvistAndel(YtelseAnvistAndel ytelseAnvistAndel) {
        this.ytelseAnvist.leggTilYtelseAnvistAndel(ytelseAnvistAndel);
        return this;
    }


    public YtelseAnvist build() {
        return ytelseAnvist;
    }

}
