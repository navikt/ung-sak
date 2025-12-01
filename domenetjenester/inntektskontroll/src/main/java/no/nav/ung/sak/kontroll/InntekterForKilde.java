package no.nav.ung.sak.kontroll;

import no.nav.ung.kodeverk.arbeidsforhold.InntektYtelseType;
import no.nav.ung.sak.typer.Arbeidsgiver;

import java.util.List;

public record InntekterForKilde(
    InntektType inntektType,
    Arbeidsgiver arbeidsgiver,
    InntektYtelseType ytelseType,
    List<Inntektsperiode> inntekter
) {

    static InntekterForKilde forBrukersRapporterteArbeidsinntekter(Inntektsperiode inntektsperiode) {
        return new InntekterForKilde(
            InntektType.ARBEIDSTAKER_ELLER_FRILANSER,
            null,
            null,
            List.of(inntektsperiode)
        );
    }

}

