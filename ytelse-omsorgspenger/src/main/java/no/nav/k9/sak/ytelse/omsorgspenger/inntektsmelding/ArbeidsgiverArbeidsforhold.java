package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.Objects;

import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

class ArbeidsgiverArbeidsforhold {

    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforhold;

    public ArbeidsgiverArbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforhold) {
        this.arbeidsgiver = Objects.requireNonNull(arbeidsgiver);
        this.arbeidsforhold = arbeidsforhold;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforhold() {
        return arbeidsforhold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArbeidsgiverArbeidsforhold that = (ArbeidsgiverArbeidsforhold) o;
        return Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(arbeidsforhold, that.arbeidsforhold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidsforhold);
    }
}
