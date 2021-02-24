package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.Objects;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class Aktivitet {
    private UttakArbeidType aktivitetType;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;

    public Aktivitet(UttakArbeidType aktivitetType, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        this.aktivitetType = aktivitetType;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = Objects.requireNonNull(arbeidsforholdRef);
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public boolean matcher(Aktivitet aktivitet) {
        return Objects.equals(aktivitetType, aktivitet.aktivitetType) &&
            Objects.equals(arbeidsgiver, aktivitet.arbeidsgiver) &&
            arbeidsforholdRef.gjelderFor(aktivitet.arbeidsforholdRef);
    }

    @Override
    public String toString() {
        return "Arbeidsforhold{" +
            "aktivitetType=" + aktivitetType +
            "arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Aktivitet that = (Aktivitet) o;
        return Objects.equals(aktivitetType, that.aktivitetType) &&
            Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktivitetType, arbeidsgiver, arbeidsforholdRef);
    }
}
