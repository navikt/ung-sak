package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.uttak;

import java.util.Objects;

import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;

class WrappedUttak {

    private UttakPeriode periode;

    public WrappedUttak(UttakPeriode periode) {
        this.periode = periode;
    }

    public UttakPeriode getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedUttak that = (WrappedUttak) o;
        return Objects.equals(periode.getTimerPleieAvBarnetPerDag(), that.periode.getTimerPleieAvBarnetPerDag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode.getTimerPleieAvBarnetPerDag());
    }
}
