package no.nav.k9.sak.perioder;

import java.util.Objects;
import java.util.function.BiPredicate;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VurdertSøktPeriode.SøktPeriodeData;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class VurdertSøktPeriode<T extends SøktPeriodeData> {

    private DatoIntervallEntitet periode;
    private UttakArbeidType type;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private Utfall utfall;
    private T raw;

    /**
     * Ingen faktisk krav om utbetaling, men en periode for vurdering
     *
     * @param periode fom-tom
     * @param raw entitet
     */
    public VurdertSøktPeriode(DatoIntervallEntitet periode, Utfall utfall, T raw) {
        this.periode = periode;
        this.utfall = utfall;
        this.raw = raw;
    }

    public VurdertSøktPeriode(DatoIntervallEntitet periode, UttakArbeidType type, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, Utfall utfall, T raw) {
        this.periode = periode;
        this.type = type;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.utfall = utfall;
        this.raw = raw;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public UttakArbeidType getType() {
        return type;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public Utfall getUtfall() {
        return utfall;
    }

    public T getRaw() {
        return raw;
    }

    public VurdertSøktPeriode<T> justerUtfall(Utfall oppdatertUtfall) {
        return new VurdertSøktPeriode<>(periode, type, arbeidsgiver, arbeidsforholdRef, oppdatertUtfall, raw);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var that = (VurdertSøktPeriode) o;
        return type == that.type &&
            Objects.equals(arbeidsgiver, that.arbeidsgiver)
            && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef)
            && Objects.equals(raw, that.raw)
            && utfall == that.utfall;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, arbeidsgiver, arbeidsforholdRef, utfall, raw);
    }

    @Override
    public String toString() {
        return "VurdertSøktPeriode{" +
            "periode=" + periode +
            ", type=" + type +
            ", arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", utfall=" + utfall +
            '}';
    }

    public static <T extends SøktPeriodeData> LocalDateTimeline<VurdertSøktPeriode<T>> compress(LocalDateTimeline<VurdertSøktPeriode<T>> timeline, RawCreator<T> rawCreator) {
        BiPredicate<VurdertSøktPeriode<T>, VurdertSøktPeriode<T>> equalsChecker = (VurdertSøktPeriode<T> v1, VurdertSøktPeriode<T> v2) -> {
            return Objects.equals(v1.getArbeidsgiver(), v2.getArbeidsgiver())
                && Objects.equals(v1.getArbeidsforholdRef(), v2.getArbeidsforholdRef())
                && Objects.equals(v1.getType(), v2.getType())
                && Objects.equals(v1.getUtfall(), v2.getUtfall())
                && Objects.equals(v1.getRaw().getPayload(), v2.getRaw().getPayload()); // merk bruker payload her og ikke kun raw
        };
        LocalDateSegmentCombinator<VurdertSøktPeriode<T>, VurdertSøktPeriode<T>, VurdertSøktPeriode<T>> nySegment = (i, v1, v2) -> {
            var v = v1.getValue();
            var di = DatoIntervallEntitet.fra(i);
            if (Objects.equals(v1, v2)) {
                return v1;
            } else {
                T nyRaw = rawCreator.apply(i, v1.getValue().getRaw(), v2.getValue().getRaw());
                return new LocalDateSegment<>(i, new VurdertSøktPeriode<>(di, v.getType(), v.getArbeidsgiver(), v.getArbeidsforholdRef(), v.getUtfall(), nyRaw));
            }
        };
        return timeline.compress(equalsChecker, nySegment);
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
