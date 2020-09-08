package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.util.Tuple;

public interface YtelsespesifikkeInntektsmeldingTjeneste {

    Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(BehandlingReferanse behandlingReferanse);

    Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erMottattInntektsmeldingUtenArbeidsforhold(BehandlingReferanse behandlingReferanse);
}
