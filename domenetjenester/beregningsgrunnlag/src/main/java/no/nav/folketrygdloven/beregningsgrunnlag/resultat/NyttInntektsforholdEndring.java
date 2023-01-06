package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.util.Objects;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class NyttInntektsforholdEndring {

    private final AktivitetStatus aktivitetStatus;
    private final Arbeidsgiver arbeidsgiver;
    private final BeløpEndring bruttoInntektPrÅrEndring;
    private final ToggleEndring skalRedusereUtbetalingEndring;

    public NyttInntektsforholdEndring(AktivitetStatus aktivitetStatus, Arbeidsgiver arbeidsgiver, BeløpEndring bruttoInntektPrÅrEndring, ToggleEndring skalRedusereUtbetalingEndring) {
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsgiver = arbeidsgiver;
        this.bruttoInntektPrÅrEndring = bruttoInntektPrÅrEndring;
        this.skalRedusereUtbetalingEndring = skalRedusereUtbetalingEndring;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public BeløpEndring getBruttoInntektPrÅrEndring() {
        return bruttoInntektPrÅrEndring;
    }

    public ToggleEndring getSkalRedusereUtbetalingEndring() {
        return skalRedusereUtbetalingEndring;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NyttInntektsforholdEndring that = (NyttInntektsforholdEndring) o;
        return aktivitetStatus == that.aktivitetStatus && Objects.equals(arbeidsgiver, that.arbeidsgiver) && Objects.equals(bruttoInntektPrÅrEndring, that.bruttoInntektPrÅrEndring) && skalRedusereUtbetalingEndring.equals(that.skalRedusereUtbetalingEndring);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktivitetStatus, arbeidsgiver, bruttoInntektPrÅrEndring, skalRedusereUtbetalingEndring);
    }
}
