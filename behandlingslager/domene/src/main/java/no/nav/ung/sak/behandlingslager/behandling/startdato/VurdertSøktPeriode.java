package no.nav.ung.sak.behandlingslager.behandling.startdato;

import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.behandlingslager.behandling.startdato.VurdertSøktPeriode.SøktPeriodeData;

public class VurdertSøktPeriode<T extends SøktPeriodeData> {

    private DatoIntervallEntitet periode;
    private Utfall utfall;
    private T raw;

    /**
     * Ingen faktisk krav om utbetaling, men en periode for vurdering
     *
     * @param periode fom-tom
     * @param raw     entitet
     */
    public VurdertSøktPeriode(DatoIntervallEntitet periode, Utfall utfall, T raw) {
        this.periode = periode;
        this.utfall = utfall;
        this.raw = raw;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Utfall getUtfall() {
        return utfall;
    }

    public T getRaw() {
        return raw;
    }

    public VurdertSøktPeriode<T> justerUtfall(Utfall oppdatertUtfall) {
        return new VurdertSøktPeriode<>(periode, oppdatertUtfall, raw);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var that = (VurdertSøktPeriode) o;
        return Objects.equals(raw, that.raw)
            && utfall == that.utfall;
    }

    @Override
    public int hashCode() {
        return Objects.hash(utfall, raw);
    }

    @Override
    public String toString() {
        return "VurdertSøktPeriode{" +
            "periode=" + periode +
            ", utfall=" + utfall +
            '}';
    }

    @FunctionalInterface
    public interface SøktPeriodeData {
        <V> V getPayload();
    }

    @FunctionalInterface
    public interface RawCreator<V extends SøktPeriodeData> {
        V apply(LocalDateInterval t, V v1, V v2);

    }

}
