package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.Objects;

import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

class WrappedOppgittFraværPeriode {
    private OppgittFraværPeriode periode;
    private Aktivitet aktivitet;
    private boolean avslått;

    public WrappedOppgittFraværPeriode(OppgittFraværPeriode periode, boolean avslått) {
        this.periode = periode;
        if (periode != null && periode.getAktivitetType() != null) {
            this.aktivitet = new Aktivitet(periode.getAktivitetType(), periode.getArbeidsgiver(), periode.getArbeidsforholdRef());
        } else {
            this.aktivitet = null;
        }
        this.avslått = avslått;
    }

    public OppgittFraværPeriode getPeriode() {
        return periode;
    }

    public boolean getErAvslått() {
        return avslått;
    }

    public Aktivitet getAktivitet() {
        return aktivitet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedOppgittFraværPeriode that = (WrappedOppgittFraværPeriode) o;
        return avslått == that.avslått
            && Objects.equals(aktivitet, that.aktivitet)
            && periodeEquals(that);
    }

    private boolean periodeEquals(WrappedOppgittFraværPeriode that) {
        if (this.periode != null && that.periode != null) {
            return Objects.equals(periode.getFraværPerDag(), that.periode.getFraværPerDag())
                && Objects.equals(periode.getAktivitetType(), that.periode.getAktivitetType());
        } else
            return this.periode == null && that.periode == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode.getFraværPerDag(), periode.getAktivitetType(), aktivitet, avslått);
    }

    @Override
    public String toString() {
        return "WrappedOppgittFraværPeriode{" +
            "periode=" + periode +
            ", avslått=" + avslått +
            '}';
    }

}
