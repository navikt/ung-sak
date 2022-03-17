package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.Objects;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.Arbeidsgiver;

public record AktivitetTypeArbeidsgiver(
    UttakArbeidType aktivitetType,
    Arbeidsgiver arbeidsgiver) {

    public AktivitetTypeArbeidsgiver {
        if (aktivitetType == UttakArbeidType.ARBEIDSTAKER) {
            Objects.requireNonNull(arbeidsgiver);
        }
        if (aktivitetType == UttakArbeidType.FRILANSER && arbeidsgiver != null) {
            throw new IllegalArgumentException("Skal ikke sette arbeidsforhold for FRILANSER her");
        }
    }

}
