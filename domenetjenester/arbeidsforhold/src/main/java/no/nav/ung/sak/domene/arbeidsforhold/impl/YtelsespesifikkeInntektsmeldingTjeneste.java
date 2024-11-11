package no.nav.ung.sak.domene.arbeidsforhold.impl;

import java.util.Map;
import java.util.Set;

import no.nav.ung.sak.typer.Arbeidsgiver;

public interface YtelsespesifikkeInntektsmeldingTjeneste {

    Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erMottattInntektsmeldingUtenArbeidsforhold(InntektsmeldingVurderingInput input);

    Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erOvergangMedArbeidsforholdsIdHosSammeArbeidsgiver(InntektsmeldingVurderingInput input);
}
