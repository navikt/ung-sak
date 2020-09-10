package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.Objects;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;

class AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold {

    private UttakArbeidType aktivitetType;
    private ArbeidsgiverArbeidsforhold arbeidsgiverArbeidsforhold;

    public AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold(UttakArbeidType aktivitetType, ArbeidsgiverArbeidsforhold arbeidsgiverArbeidsforhold) {
        this.aktivitetType = Objects.requireNonNull(aktivitetType);
        this.arbeidsgiverArbeidsforhold = Objects.requireNonNull(arbeidsgiverArbeidsforhold);
    }

    public UttakArbeidType getAktivitetType() {
        return aktivitetType;
    }

    public ArbeidsgiverArbeidsforhold getArbeidsgiverArbeidsforhold() {
        return arbeidsgiverArbeidsforhold;
    }

    public boolean matcher(AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold aktivitet) {
        if (!aktivitetType.equals(aktivitet.getAktivitetType())) {
            return false;
        }

        return arbeidsgiverArbeidsforhold.matcher(aktivitet.getArbeidsgiverArbeidsforhold());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold that = (AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold) o;
        return aktivitetType == that.aktivitetType &&
            Objects.equals(arbeidsgiverArbeidsforhold, that.arbeidsgiverArbeidsforhold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktivitetType, arbeidsgiverArbeidsforhold);
    }
}
