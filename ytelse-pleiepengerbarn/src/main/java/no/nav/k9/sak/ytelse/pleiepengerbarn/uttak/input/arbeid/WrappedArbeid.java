package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.util.Objects;

import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;

class WrappedArbeid {

    private final ArbeidPeriode periode;

    public WrappedArbeid(ArbeidPeriode periode) {
        this.periode = periode;
    }

    public ArbeidPeriode getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedArbeid that = (WrappedArbeid) o;
        return Objects.equals(periode.getAktivitetType(), that.periode.getAktivitetType())
            && Objects.equals(periode.getArbeidsgiver(), that.periode.getArbeidsgiver())
            && Objects.equals(periode.getArbeidsforholdRef(), that.periode.getArbeidsforholdRef())
            && Objects.equals(periode.getFaktiskArbeidTimerPerDag(), that.periode.getFaktiskArbeidTimerPerDag())
            && Objects.equals(periode.getJobberNormaltTimerPerDag(), that.periode.getJobberNormaltTimerPerDag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode.getAktivitetType(),
            periode.getArbeidsgiver(),
            periode.getArbeidsforholdRef(),
            periode.getFaktiskArbeidTimerPerDag(),
            periode.getJobberNormaltTimerPerDag());
    }
}
