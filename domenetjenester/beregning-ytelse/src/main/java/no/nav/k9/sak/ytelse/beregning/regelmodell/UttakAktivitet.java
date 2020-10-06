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

    public boolean gjelderSamme(Arbeidsforhold arbeidsforhold) {
        if ((this.arbeidsforhold == null || arbeidsforhold == null)
            || (this.arbeidsforhold.erFrilanser() || arbeidsforhold.erFrilanser())) {
            return Objects.equals(this.arbeidsforhold, arbeidsforhold);
        }
        return Objects.equals(this.arbeidsforhold.getIdentifikator(), arbeidsforhold.getIdentifikator())
            && this.arbeidsforhold.getArbeidsforholdId().gjelderFor(arbeidsforhold.getArbeidsforholdId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<type=" + type + ", arbeid=" + arbeidsforhold + ", utbetalingsgrad=" + utbetalingsgrad + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var other = (UttakAktivitet) obj;
        return Objects.equals(type, other.type)
            && Objects.equals(arbeidsforhold, other.arbeidsforhold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, arbeidsforhold);
    }
}
