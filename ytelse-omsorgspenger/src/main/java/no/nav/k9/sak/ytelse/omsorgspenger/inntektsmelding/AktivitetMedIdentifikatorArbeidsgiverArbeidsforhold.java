package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.Objects;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold {

    private UttakArbeidType aktivitetType;
    private ArbeidsgiverArbeidsforhold arbeidsgiverArbeidsforhold;

    private AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold(UttakArbeidType aktivitetType) {
        this(aktivitetType, null);
    }

    private AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold(UttakArbeidType aktivitetType, ArbeidsgiverArbeidsforhold arbeidsgiverArbeidsforhold) {
        this.aktivitetType = Objects.requireNonNull(aktivitetType);
        this.arbeidsgiverArbeidsforhold = arbeidsgiverArbeidsforhold;
        if (aktivitetType == UttakArbeidType.ARBEIDSTAKER) {
            Objects.requireNonNull(arbeidsgiverArbeidsforhold);
        }
        if (aktivitetType == UttakArbeidType.FRILANSER && arbeidsgiverArbeidsforhold != null) {
            throw new IllegalArgumentException("Skal ikke sette arbeidsforhold for FRILANSER her");
        }
    }

    public static AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold lagAktivitetIdentifikator(SøktPeriode<OppgittFraværPeriode> søktPeriode) {
        if (søktPeriode.getArbeidsgiver() != null) {
            return new AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold(søktPeriode.getType(), new ArbeidsgiverArbeidsforhold(søktPeriode.getArbeidsgiver(), søktPeriode.getArbeidsforholdRef()));
        } else {
            return new AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold(søktPeriode.getType());
        }
    }

    public static AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold lagAktivitetIdentifikator(VurdertSøktPeriode<OppgittFraværPeriode> vurdertSøktPeriode) {
        if (vurdertSøktPeriode.getArbeidsgiver() != null) {
            return new AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold(vurdertSøktPeriode.getType(), new ArbeidsgiverArbeidsforhold(vurdertSøktPeriode.getArbeidsgiver(), vurdertSøktPeriode.getArbeidsforholdRef()));
        } else {
            return new AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold(vurdertSøktPeriode.getType());
        }
    }


    public UttakArbeidType getAktivitetType() {
        return aktivitetType;
    }

    public ArbeidsgiverArbeidsforhold getArbeidsgiverArbeidsforhold() {
        return arbeidsgiverArbeidsforhold;
    }

    public boolean gjelderSamme(AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold aktivitet) {
        if (!aktivitetType.equals(aktivitet.getAktivitetType())) {
            return false;
        }

        return arbeidsgiverArbeidsforhold == aktivitet.getArbeidsgiverArbeidsforhold() || arbeidsgiverArbeidsforhold.identifisererSamme(aktivitet.getArbeidsgiverArbeidsforhold());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold) o;
        return aktivitetType == that.aktivitetType &&
            Objects.equals(arbeidsgiverArbeidsforhold, that.arbeidsgiverArbeidsforhold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktivitetType, arbeidsgiverArbeidsforhold);
    }
}
