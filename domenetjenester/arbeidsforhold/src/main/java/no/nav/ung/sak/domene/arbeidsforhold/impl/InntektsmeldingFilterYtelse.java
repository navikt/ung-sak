package no.nav.ung.sak.domene.arbeidsforhold.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.typer.Arbeidsgiver;

public interface InntektsmeldingFilterYtelse {

    /** Returnerer påkrevde inntektsmeldinger etter ytelsesspesifikke vurdering og filtrering */
    <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelse(BehandlingReferanse referanse, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                    Map<Arbeidsgiver, Set<V>> påkrevde);

    <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelseUtvidet(BehandlingReferanse referanse, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                        Map<Arbeidsgiver, Set<V>> påkrevde);
}
