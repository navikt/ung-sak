package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.util.Tuple;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultManglendePåkrevdeInntektsmeldingerTjeneste implements YtelsespesifikkeInntektsmeldingTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultManglendePåkrevdeInntektsmeldingerTjeneste.class);

    @Inject
    public DefaultManglendePåkrevdeInntektsmeldingerTjeneste() {
        // CDI
    }

    @Override
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erMottattInntektsmeldingUtenArbeidsforhold(InntektsmeldingVurderingInput input) {
        var result = new HashMap<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>>();
        var grunnlagOpt = Optional.ofNullable(input.getGrunnlag());
        if (grunnlagOpt.isEmpty()) {
            return result;
        }
        var grunnlag = grunnlagOpt.get();
        final Optional<InntektsmeldingAggregat> inntektsmeldinger = grunnlag.getInntektsmeldinger();
        if (inntektsmeldinger.isPresent()) {
            final InntektsmeldingAggregat aggregat = inntektsmeldinger.get();
            for (Inntektsmelding inntektsmelding : aggregat.getInntektsmeldingerSomSkalBrukes()) {
                final Tuple<Long, Long> antallArbeidsforIArbeidsgiveren = antallArbeidsforHosArbeidsgiveren(grunnlag,
                    inntektsmelding.getArbeidsgiver(),
                    inntektsmelding.getArbeidsforholdRef(), input.getReferanse().getAktørId(), input.getReferanse().getUtledetSkjæringstidspunkt());
                if (antallArbeidsforIArbeidsgiveren.getElement1() == 0 && antallArbeidsforIArbeidsgiveren.getElement2() == 0
                    && IkkeTattStillingTil.vurder(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef(), grunnlag)) {
                    final Arbeidsgiver arbeidsgiver = inntektsmelding.getArbeidsgiver();
                    final Set<InternArbeidsforholdRef> arbeidsforholdRefs = trekkUtRef(inntektsmelding);
                    LeggTilResultat.leggTil(result, AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD, arbeidsgiver, arbeidsforholdRefs);
                    LOGGER.info("Inntektsmelding uten kjent arbeidsforhold: arbeidsforholdRef={}", arbeidsforholdRefs);
                }
            }
        }
        return result;
    }

    @Override
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erOvergangMedArbeidsforholdsIdHosSammeArbeidsgiver(InntektsmeldingVurderingInput input) {
        return Map.of();
    }

    private long antallArbeidsfor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, YrkesaktivitetFilter filter) {
        return filter.getYrkesaktiviteter()
            .stream()
            .filter(yr -> yr.erArbeidsforhold()
                && yr.getArbeidsgiver().equals(arbeidsgiver)
                && yr.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef))
            .count();
    }

    private Tuple<Long, Long> antallArbeidsforHosArbeidsgiveren(InntektArbeidYtelseGrunnlag grunnlag,
                                                                Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef, AktørId aktørId, LocalDate skjæringstidspunkt) {
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId));

        long antallFør = antallArbeidsfor(arbeidsgiver, arbeidsforholdRef, filter.før(skjæringstidspunkt));
        long antallEtter = antallArbeidsfor(arbeidsgiver, arbeidsforholdRef, filter.etter(skjæringstidspunkt));

        return new Tuple<>(antallFør, antallEtter);
    }

    private Set<InternArbeidsforholdRef> trekkUtRef(Inntektsmelding inntektsmelding) {
        if (inntektsmelding.gjelderForEtSpesifiktArbeidsforhold()) {
            return Stream.of(inntektsmelding.getArbeidsforholdRef()).collect(Collectors.toSet());
        }
        return Stream.of(InternArbeidsforholdRef.nullRef()).collect(Collectors.toSet());
    }

}
