package no.nav.k9.sak.ytelse.beregning.regelmodell;

import java.math.BigDecimal;
import java.util.Objects;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

public class UttakAktivitet {
    private BigDecimal stillingsgrad;
    private BigDecimal utbetalingsgrad;
    private Arbeidsforhold arbeidsforhold;
    private UttakArbeidType type;
    private boolean erGradering;

    public UttakAktivitet(BigDecimal stillingsgrad, BigDecimal utbetalingsgrad, Arbeidsforhold arbeidsforhold, UttakArbeidType type, boolean erGradering) {
        this.erGradering = erGradering;
        this.stillingsgrad = Objects.requireNonNull(stillingsgrad, "stillingsgrad");
        this.utbetalingsgrad = Objects.requireNonNull(utbetalingsgrad, "utbetalingsgrad");
        this.arbeidsforhold = arbeidsforhold;
        this.type = type;
    }

    public BigDecimal getStillingsgrad() {
        return stillingsgrad;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public Arbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    public UttakArbeidType getType() {
        return type;
    }

    public boolean isErGradering() {
        return erGradering;
    }
}
