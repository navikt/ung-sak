package no.nav.ung.sak.søknadsfrist;

import java.util.Objects;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public class SøktPeriode<T> {

    private DatoIntervallEntitet periode;
    private T raw;

    /**
     * Ingen faktisk krav om utbetaling, men en periode for vurdering
     *
     * @param periode fom-tom
     * @param raw entitet
     */
    public SøktPeriode(DatoIntervallEntitet periode, T raw) {
        this.periode = periode;
        this.raw = raw;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public T getRaw() {
        return raw;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var that = (SøktPeriode) o;
        return Objects.equals(raw, that.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw);
    }

    @Override
    public String toString() {
        return "SøktPeriode{" +
            "periode=" + periode +
            '}';
    }
}
