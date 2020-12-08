package no.nav.k9.sak.perioder;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

import java.util.Objects;

public class SøktPeriode {

    private DatoIntervallEntitet periode;
    private UttakArbeidType type;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;

    public SøktPeriode(DatoIntervallEntitet periode, UttakArbeidType type, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        this.periode = periode;
        this.type = type;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
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

    public static SøktPeriode arbeid(DatoIntervallEntitet periode, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return new SøktPeriode(periode, UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SøktPeriode that = (SøktPeriode) o;
        return Objects.equals(periode, that.periode) &&
            type == that.type &&
            Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, type, arbeidsgiver, arbeidsforholdRef);
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
