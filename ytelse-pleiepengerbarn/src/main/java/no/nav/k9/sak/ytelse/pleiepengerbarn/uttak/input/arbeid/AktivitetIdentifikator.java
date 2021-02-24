package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.util.Objects;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class AktivitetIdentifikator {

    private UttakArbeidType aktivitetType;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforhold;

    public AktivitetIdentifikator(UttakArbeidType aktivitetType, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforhold) {
        this.aktivitetType = Objects.requireNonNull(aktivitetType);
        this.arbeidsgiver = UttakArbeidType.ARBEIDSTAKER.equals(aktivitetType) ? Objects.requireNonNull(arbeidsgiver) : arbeidsgiver;
        this.arbeidsforhold = arbeidsforhold;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforhold() {
        return arbeidsforhold;
    }

    public UttakArbeidType getAktivitetType() {
        return aktivitetType;
    }

    public boolean identifisererSamme(AktivitetIdentifikator arbeidsforhold) {
        if(!aktivitetType.equals(arbeidsforhold.getAktivitetType())) {
            return false;
        }
        if (!arbeidsgiver.equals(arbeidsforhold.getArbeidsgiver())) {
            return false;
        }

        return this.arbeidsforhold.gjelderFor(arbeidsforhold.getArbeidsforhold());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AktivitetIdentifikator that = (AktivitetIdentifikator) o;
        return Objects.equals(aktivitetType, that.aktivitetType) &&
            Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(arbeidsforhold, that.arbeidsforhold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktivitetType, arbeidsgiver, arbeidsforhold);
    }
}
