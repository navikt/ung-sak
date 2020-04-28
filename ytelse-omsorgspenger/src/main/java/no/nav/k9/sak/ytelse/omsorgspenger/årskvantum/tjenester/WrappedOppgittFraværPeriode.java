package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.Objects;

import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

class WrappedOppgittFraværPeriode {
    private OppgittFraværPeriode periode;
    private boolean avslått;

    public WrappedOppgittFraværPeriode(OppgittFraværPeriode periode, boolean avslått) {
        this.periode = periode;
        this.avslått = avslått;
    }

    public OppgittFraværPeriode getPeriode() {
        return periode;
    }

    public boolean getErAvslått() {
        return avslått;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedOppgittFraværPeriode that = (WrappedOppgittFraværPeriode) o;
        return avslått == that.avslått
            && Objects.equals(periode.getFraværPerDag(), that.periode.getFraværPerDag())
            && Objects.equals(periode.getAktivitetType(), that.periode.getAktivitetType())
            && Objects.equals(periode.getArbeidsgiver(), that.periode.getArbeidsgiver())
            && Objects.equals(periode.getArbeidsforholdRef(), that.periode.getArbeidsforholdRef());
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode.getFraværPerDag(), periode.getAktivitetType(), periode.getArbeidsgiver(), periode.getArbeidsforholdRef(), avslått);
    }

    @Override
    public String toString() {
        return "WrappedOppgittFraværPeriode{" +
            "periode=" + periode +
            ", avslått=" + avslått +
            '}';
    }

}
