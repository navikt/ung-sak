package no.nav.k9.sak.perioder;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

import java.util.Objects;

public class SøktPeriode<T> {

    private DatoIntervallEntitet periode;
    private UttakArbeidType type;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private T raw;

    public SøktPeriode(DatoIntervallEntitet periode, UttakArbeidType type, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, T raw) {
        this.periode = periode;
        this.type = type;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
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

    public T getRaw() {
        return raw;
    }

    public void justerPeriode(LocalDateSegment<SøktPeriode<T>> segment) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom());
    }

    public static <T> SøktPeriode<T> arbeid(DatoIntervallEntitet periode, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, T raw) {
        return new SøktPeriode<>(periode, UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef, raw);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SøktPeriode<T> that = (SøktPeriode<T>) o;
        return type == that.type &&
            Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, arbeidsgiver, arbeidsforholdRef);
    }

    @Override
    public String toString() {
        return "SøktPeriode{" +
            "periode=" + periode +
            ", type=" + type +
            ", arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            '}';
    }
}
