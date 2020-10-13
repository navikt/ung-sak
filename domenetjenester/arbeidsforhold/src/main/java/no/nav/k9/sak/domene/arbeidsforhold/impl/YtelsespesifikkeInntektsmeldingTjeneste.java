package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.util.Map;
import java.util.Set;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.typer.Arbeidsgiver;

public interface YtelsespesifikkeInntektsmeldingTjeneste {

    Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(BehandlingReferanse behandlingReferanse);

    Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erMottattInntektsmeldingUtenArbeidsforhold(BehandlingReferanse behandlingReferanse);

    Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erOvergangMedArbeidsforholdsIdHosSammeArbeidsgiver(BehandlingReferanse ref);
}
