package no.nav.ung.sak.kontroll;

import no.nav.ung.kodeverk.arbeidsforhold.InntektYtelseType;
import no.nav.ung.sak.felles.typer.Arbeidsgiver;
import java.util.List;

public record InntekterForKilde(
    InntektType inntektType,
    Arbeidsgiver arbeidsgiver,
    InntektYtelseType ytelseType,
    List<Inntektsperiode> inntekter
) {}

