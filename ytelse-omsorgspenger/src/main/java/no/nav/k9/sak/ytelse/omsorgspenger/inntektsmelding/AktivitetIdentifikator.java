package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.Objects;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class AktivitetIdentifikator {

    private UttakArbeidType aktivitetType;
    private ArbeidsgiverArbeidsforhold arbeidsgiverArbeidsforhold;

    private AktivitetIdentifikator(UttakArbeidType aktivitetType) {
        this(aktivitetType, null);
    }

    private AktivitetIdentifikator(UttakArbeidType aktivitetType, ArbeidsgiverArbeidsforhold arbeidsgiverArbeidsforhold) {
        this.aktivitetType = Objects.requireNonNull(aktivitetType);
        this.arbeidsgiverArbeidsforhold = arbeidsgiverArbeidsforhold;
        if (aktivitetType == UttakArbeidType.ARBEIDSTAKER) {
            Objects.requireNonNull(arbeidsgiverArbeidsforhold);
        }
        if (aktivitetType == UttakArbeidType.FRILANSER && arbeidsgiverArbeidsforhold != null) {
            throw new IllegalArgumentException("Skal ikke sette arbeidsforhold for FRILANSER her");
        }
    }

    public static AktivitetIdentifikator lagAktivitetIdentifikator(SøktPeriode<OppgittFraværPeriode> søktPeriode) {
        if (søktPeriode.getArbeidsgiver() != null) {
            return new AktivitetIdentifikator(søktPeriode.getType(), new ArbeidsgiverArbeidsforhold(søktPeriode.getArbeidsgiver(), søktPeriode.getArbeidsforholdRef()));
        } else {
            return new AktivitetIdentifikator(søktPeriode.getType());
        }
    }

    public static AktivitetIdentifikator lagAktivitetIdentifikator(OppgittFraværPeriode oppgittFraværPeriode) {
        if (oppgittFraværPeriode.getArbeidsgiver() != null) {
            return new AktivitetIdentifikator(oppgittFraværPeriode.getAktivitetType(), new ArbeidsgiverArbeidsforhold(oppgittFraværPeriode.getArbeidsgiver(), oppgittFraværPeriode.getArbeidsforholdRef()));
        } else {
            return new AktivitetIdentifikator(oppgittFraværPeriode.getAktivitetType());
        }
    }

    public static AktivitetIdentifikator lagAktivitetIdentifikator(VurdertSøktPeriode<OppgittFraværPeriode> vurdertSøktPeriode) {
        if (vurdertSøktPeriode.getArbeidsgiver() != null) {
            return new AktivitetIdentifikator(vurdertSøktPeriode.getType(), new ArbeidsgiverArbeidsforhold(vurdertSøktPeriode.getArbeidsgiver(), vurdertSøktPeriode.getArbeidsforholdRef()));
        } else {
            return new AktivitetIdentifikator(vurdertSøktPeriode.getType());
        }
    }


    public UttakArbeidType getAktivitetType() {
        return aktivitetType;
    }

    public ArbeidsgiverArbeidsforhold getArbeidsgiverArbeidsforhold() {
        return arbeidsgiverArbeidsforhold;
    }

    public boolean gjelderSamme(AktivitetIdentifikator aktivitet) {
        if (!aktivitetType.equals(aktivitet.getAktivitetType())) {
            return false;
        }

        return Objects.equals(arbeidsgiverArbeidsforhold, aktivitet.getArbeidsgiverArbeidsforhold()) || arbeidsgiverArbeidsforhold.identifisererSamme(aktivitet.getArbeidsgiverArbeidsforhold());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (AktivitetIdentifikator) o;
        return aktivitetType == that.aktivitetType &&
            Objects.equals(arbeidsgiverArbeidsforhold, that.arbeidsgiverArbeidsforhold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktivitetType, arbeidsgiverArbeidsforhold);
    }
}
