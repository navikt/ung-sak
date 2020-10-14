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

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultManglendePåkrevdeInntektsmeldingerTjeneste implements YtelsespesifikkeInntektsmeldingTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultManglendePåkrevdeInntektsmeldingerTjeneste.class);

    private InntektsmeldingRegisterTjeneste inntektsmeldingTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private SøknadRepository søknadRepository;

    DefaultManglendePåkrevdeInntektsmeldingerTjeneste() {
        // CDI
    }

    @Inject
    public DefaultManglendePåkrevdeInntektsmeldingerTjeneste(InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste,
                                                             InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                             SøknadRepository søknadRepository) {
        this.inntektsmeldingTjeneste = inntektsmeldingArkivTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.søknadRepository = søknadRepository;
    }

    @Override
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(BehandlingReferanse behandlingReferanse) {
        var result = new HashMap<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>>();
        boolean erEndringssøknad = erEndringssøknad(behandlingReferanse);
        boolean erIkkeEndringssøknad = !erEndringssøknad;

        final Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> manglendeInntektsmeldinger = inntektsmeldingTjeneste
            .utledManglendeInntektsmeldingerFraGrunnlagForVurdering(behandlingReferanse, erEndringssøknad);
        if (erIkkeEndringssøknad) {
            for (Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> entry : manglendeInntektsmeldinger.entrySet()) {
                LeggTilResultat.leggTil(result, AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING, entry.getKey(), entry.getValue());
                LOGGER.info("Mangler inntektsmelding: arbeidsforholdRef={}", entry.getValue());
            }
        }
        return result;
    }

    @Override
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erMottattInntektsmeldingUtenArbeidsforhold(BehandlingReferanse behandlingReferanse) {
        var result = new HashMap<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>>();
        var grunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId());
        final Optional<InntektsmeldingAggregat> inntektsmeldinger = grunnlag.getInntektsmeldinger();
        if (inntektsmeldinger.isPresent()) {
            final InntektsmeldingAggregat aggregat = inntektsmeldinger.get();
            for (Inntektsmelding inntektsmelding : aggregat.getInntektsmeldingerSomSkalBrukes()) {
                final Tuple<Long, Long> antallArbeidsforIArbeidsgiveren = antallArbeidsforHosArbeidsgiveren(behandlingReferanse, grunnlag,
                    inntektsmelding.getArbeidsgiver(),
                    inntektsmelding.getArbeidsforholdRef());
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
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> erOvergangMedArbeidsforholdsIdHosSammeArbeidsgiver(BehandlingReferanse ref) {
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

    private Tuple<Long, Long> antallArbeidsforHosArbeidsgiveren(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag grunnlag,
                                                                Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        LocalDate skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId()));

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

    private boolean erEndringssøknad(BehandlingReferanse referanse) {
        return søknadRepository.hentSøknadHvisEksisterer(referanse.getBehandlingId())
            .map(SøknadEntitet::erEndringssøknad)
            .orElse(false);
    }

}
