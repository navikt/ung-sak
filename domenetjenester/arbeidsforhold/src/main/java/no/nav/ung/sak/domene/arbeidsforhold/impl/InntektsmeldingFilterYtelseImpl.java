package no.nav.ung.sak.domene.arbeidsforhold.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.InntektFilter;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.typer.Arbeidsgiver;

@FagsakYtelseTypeRef
@ApplicationScoped
public class InntektsmeldingFilterYtelseImpl implements InntektsmeldingFilterYtelse {

    private static final Period SJEKK_INNTEKT_PERIODE = Period.parse("P10M");

    @Inject
    public InntektsmeldingFilterYtelseImpl() {
        //
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelse(BehandlingReferanse referanse, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                           Map<Arbeidsgiver, Set<V>> påkrevde) {
        return påkrevde;
    }

    @Override
    public <V> Map<Arbeidsgiver, Set<V>> filtrerInntektsmeldingerForYtelseUtvidet(BehandlingReferanse referanse, Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                                                  Map<Arbeidsgiver, Set<V>> påkrevde) {
        if (inntektArbeidYtelseGrunnlag.isEmpty()) {
            return påkrevde;
        }
        Map<Arbeidsgiver, Set<V>> filtrert = new HashMap<>();
        Map<Arbeidsgiver, Set<Inntektspost>> inntekterPrArbgiver = hentInntekterForUtledningAvInntektsmeldinger(referanse, inntektArbeidYtelseGrunnlag.get());
        påkrevde.forEach((key, value) -> {
            if (inntekterPrArbgiver.get(key) != null && !inntekterPrArbgiver.get(key).isEmpty()) {
                filtrert.put(key, value);
            }
        });
        // Ligg til annen logikk, som fx utelate arbeidsforhold med stillingsprosent 0.
        return filtrert;
    }

    private Map<Arbeidsgiver, Set<Inntektspost>> hentInntekterForUtledningAvInntektsmeldinger(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag grunnlag) {
        LocalDate inntektsPeriodeFom = referanse.getUtledetSkjæringstidspunkt().minus(SJEKK_INNTEKT_PERIODE);
        Map<Arbeidsgiver, Set<Inntektspost>> inntekterPrArbgiver = new HashMap<>();

        var filter = grunnlag.getAktørInntektFraRegister(referanse.getAktørId()).map(ai -> new InntektFilter(ai).før(referanse.getUtledetSkjæringstidspunkt())).orElse(InntektFilter.EMPTY);

        filter.getAlleInntektPensjonsgivende()
            .forEach(inntekt -> {
            Set<Inntektspost> poster = filter.filtrer(inntekt, inntekt.getAlleInntektsposter()).stream()
                .filter(ip -> !InntektspostType.YTELSE.equals(ip.getInntektspostType()))
                .filter(ip -> ip.getPeriode().getFomDato().isAfter(inntektsPeriodeFom))
                .filter(it -> it.getBeløp().getVerdi().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toSet());
            if (inntekterPrArbgiver.get(inntekt.getArbeidsgiver()) != null) {
                inntekterPrArbgiver.get(inntekt.getArbeidsgiver()).addAll(poster);
            } else {
                inntekterPrArbgiver.put(inntekt.getArbeidsgiver(), poster);
            }
        });
        return inntekterPrArbgiver;
    }
}
