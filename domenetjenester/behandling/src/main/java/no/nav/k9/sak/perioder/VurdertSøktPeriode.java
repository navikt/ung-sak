package no.nav.k9.sak.perioder;

import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class VurdertSøktPeriode<T> {

    private DatoIntervallEntitet periode;
    private UttakArbeidType type;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private Utfall utfall;
    private T raw;

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

    public void justerUtfall(Utfall utfall) {
        this.utfall = utfall;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (VurdertSøktPeriode) o;
        return type == that.type &&
            Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef) &&
            utfall == that.utfall;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, arbeidsgiver, arbeidsforholdRef, utfall);
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

}
