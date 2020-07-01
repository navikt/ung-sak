package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.Objects;

import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

class WrappedOppgittFraværPeriode {
    private OppgittFraværPeriode periode;

    public WrappedOppgittFraværPeriode(OppgittFraværPeriode periode) {
        this.periode = periode;
    }

    public OppgittFraværPeriode getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedOppgittFraværPeriode that = (WrappedOppgittFraværPeriode) o;
        return periodeEquals(that);
    }

    private boolean periodeEquals(WrappedOppgittFraværPeriode that) {
        if (this.periode != null && that.periode != null) {
            return Objects.equals(periode.getFraværPerDag(), that.periode.getFraværPerDag())
                && Objects.equals(periode.getAktivitetType(), that.periode.getAktivitetType())
                && Objects.equals(periode.getArbeidsgiver(), that.periode.getArbeidsgiver())
                && Objects.equals(periode.getArbeidsforholdRef(), that.periode.getArbeidsforholdRef());
        } else
            return this.periode == null && that.periode == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode.getFraværPerDag(), periode.getAktivitetType(), periode.getArbeidsgiver(), periode.getArbeidsforholdRef());
    }

    @Override
    public String toString() {
        return "WrappedOppgittFraværPeriode{" +
            "periode=" + periode +
            '}';
    }

}
