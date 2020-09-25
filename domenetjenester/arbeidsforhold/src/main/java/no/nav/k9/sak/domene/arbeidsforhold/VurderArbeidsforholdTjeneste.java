package no.nav.k9.sak.domene.arbeidsforhold;

import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidType.FORENKLET_OPPGJØRSORDNING;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidType.MARITIMT_ARBEIDSFORHOLD;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMedÅrsak;
import no.nav.k9.sak.domene.arbeidsforhold.impl.YtelsespesifikkeInntektsmeldingTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class VurderArbeidsforholdTjeneste {

    private static final Set<ArbeidType> ARBEIDSFORHOLD_TYPER = Stream.of(ORDINÆRT_ARBEIDSFORHOLD, FORENKLET_OPPGJØRSORDNING, MARITIMT_ARBEIDSFORHOLD)
        .collect(Collectors.toSet());
    private static final Logger logger = LoggerFactory.getLogger(VurderArbeidsforholdTjeneste.class);

    private Instance<YtelsespesifikkeInntektsmeldingTjeneste> påkrevdeInntektsmeldingerTjenester;

    VurderArbeidsforholdTjeneste() {
        // CDI
    }

    @Inject
    public VurderArbeidsforholdTjeneste(@Any Instance<YtelsespesifikkeInntektsmeldingTjeneste> påkrevdeInntektsmeldingerTjenester) {
        this.påkrevdeInntektsmeldingerTjenester = påkrevdeInntektsmeldingerTjenester;
    }

    private static Set<InternArbeidsforholdRef> mapTilArbeidsforholdRef(Map.Entry<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> entry) {
        return entry.getValue().stream()
            .map(ArbeidsforholdMedÅrsak::getRef)
            .collect(Collectors.toSet());
    }

    /**
     * Vurderer alle arbeidsforhold innhentet i en behandling.
     * <p>
     * Gjør vurderinger for å se om saksbehandler må ta stilling til enkelte av disse og returener sett med hvilke
     * saksbehandler må ta stilling til.
     * <p>
     *
     * @param behandlingReferanse                    behandlingen
     * @param iayGrunnlag                            - grunnlag for behandlingen
     * @return Arbeidsforholdene det må tas stilling til
     */
    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder(BehandlingReferanse behandlingReferanse,
                                                                  InntektArbeidYtelseGrunnlag iayGrunnlag) {
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> arbeidsgiverSetMap = vurderMedÅrsak(behandlingReferanse, iayGrunnlag);
        return arbeidsgiverSetMap.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, VurderArbeidsforholdTjeneste::mapTilArbeidsforholdRef));
    }

    /**
     * Vurderer alle arbeidsforhold innhentet i en behandling.
     * <p>
     * Gjør vurderinger for å se om saksbehandler må ta stilling til enkelte av disse og returener sett med hvilke
     * saksbehandler må ta stilling til.
     * <p>
     * Legger også på en årsak for hvorfor arbeidsforholdet har fått et aksjonspunkt.
     * <p>
     *
     * @param ref                                    behandlingen
     * @param iayGrunnlag                            I(nntekt)A(rbeid)Y(telse) grunnlaget
     * @return Arbeidsforholdene det må tas stilling til
     */
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> vurderMedÅrsak(BehandlingReferanse ref,
                                                                         InntektArbeidYtelseGrunnlag iayGrunnlag) {

        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();

        // Ikke relevant å sjekke permisjonen da dette går til avslag ..
        //VurderPermisjonTjeneste.leggTilArbeidsforholdMedRelevantPermisjon(ref, result, iayGrunnlag);
        var ytelsespesifikkeInntektsmeldingTjeneste = finnPåkrevdeInntektsmeldingerTjeneste(ref);
        leggTilManglendePåkrevdeInntektsmeldinger(ref, result, ytelsespesifikkeInntektsmeldingTjeneste);
        erRapportertNormalInntektUtenArbeidsforhold(iayGrunnlag, ref);
        erMottattInntektsmeldingUtenArbeidsforhold(result, ref, ytelsespesifikkeInntektsmeldingTjeneste);

        return result;

    }

    private void leggTilManglendePåkrevdeInntektsmeldinger(BehandlingReferanse ref, Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result, YtelsespesifikkeInntektsmeldingTjeneste ytelsespesifikkeInntektsmeldingTjeneste) {
        var manglendePåkrevdeInntektsmeldinger = ytelsespesifikkeInntektsmeldingTjeneste.leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(ref);
        manglendePåkrevdeInntektsmeldinger.forEach((k, v) -> result.merge(k, v, this::mergeSets));
    }

    private Set<ArbeidsforholdMedÅrsak> mergeSets(Set<ArbeidsforholdMedÅrsak> a, Set<ArbeidsforholdMedÅrsak> b) {
        a.addAll(b);
        return a;
    }

    private YtelsespesifikkeInntektsmeldingTjeneste finnPåkrevdeInntektsmeldingerTjeneste(BehandlingReferanse ref) {
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(påkrevdeInntektsmeldingerTjenester, ref.getFagsakYtelseType());
        return tjeneste.orElseThrow(() -> new IllegalStateException("Finner ikke implementasjon for PåkrevdeInntektsmeldingerTjeneste for behandling " + ref.getBehandlingUuid()));
    }

    private void erMottattInntektsmeldingUtenArbeidsforhold(Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result,
                                                            BehandlingReferanse behandlingReferanse,
                                                            YtelsespesifikkeInntektsmeldingTjeneste ytelsespesifikkeInntektsmeldingTjeneste) {

        var resultMap = ytelsespesifikkeInntektsmeldingTjeneste.erMottattInntektsmeldingUtenArbeidsforhold(behandlingReferanse);
        resultMap.forEach((k, v) -> result.merge(k, v, this::mergeSets));
    }

    private void erRapportertNormalInntektUtenArbeidsforhold(InntektArbeidYtelseGrunnlag grunnlag, BehandlingReferanse referanse) {
        LocalDate skjæringstidspunkt = referanse.getUtledetSkjæringstidspunkt();
        var filter = grunnlag.getAktørInntektFraRegister(referanse.getAktørId()).map(ai -> new InntektFilter(ai).før(skjæringstidspunkt))
            .orElse(InntektFilter.EMPTY);

        var lønnFilter = filter.filterPensjonsgivende().filter(InntektspostType.LØNN);
        var arbeidsforholdInformasjon = grunnlag.getArbeidsforholdInformasjon();
        var filterYrkesaktivitet = new YrkesaktivitetFilter(arbeidsforholdInformasjon, grunnlag.getAktørArbeidFraRegister(referanse.getAktørId()));

        lønnFilter.getAlleInntekter().forEach(inntekt -> rapporterHvisHarIkkeArbeidsforhold(grunnlag, inntekt, filterYrkesaktivitet, skjæringstidspunkt));
    }

    private void rapporterHvisHarIkkeArbeidsforhold(InntektArbeidYtelseGrunnlag grunnlag,
                                                    Inntekt inntekt,
                                                    YrkesaktivitetFilter filterYrkesaktivitet,
                                                    LocalDate skjæringstidspunkt) {
        var filterFør = filterYrkesaktivitet.før(skjæringstidspunkt);
        var filterEtter = filterYrkesaktivitet.etter(skjæringstidspunkt);

        boolean ingenFør = true;
        if (!filterFør.getYrkesaktiviteter().isEmpty()) {
            ingenFør = ikkeArbeidsforholdRegisterert(inntekt, filterFør);
        }

        boolean ingenEtter = true;
        if (!filterEtter.getYrkesaktiviteter().isEmpty()) {
            ingenEtter = ikkeArbeidsforholdRegisterert(inntekt, filterEtter);
        }

        if (ingenFør && ingenEtter) {
            Set<InternArbeidsforholdRef> arbeidsforholdRefs = Stream.of(InternArbeidsforholdRef.nullRef()).collect(Collectors.toSet());
            Optional<InntektsmeldingAggregat> inntektsmeldinger = grunnlag.getInntektsmeldinger();
            if (inntektsmeldinger.isPresent()) {
                arbeidsforholdRefs = inntektsmeldinger.get()
                    .getInntektsmeldingerFor(inntekt.getArbeidsgiver())
                    .stream()
                    .map(Inntektsmelding::getArbeidsforholdRef)
                    .collect(Collectors.toSet());
            }
            logger.info("Inntekter uten kjent arbeidsforhold: arbeidsforholdRef={}", arbeidsforholdRefs);
        }

    }

    private boolean ikkeArbeidsforholdRegisterert(Inntekt inntekt, YrkesaktivitetFilter filter) {
        // må også sjekke mot frilans. Skal ikke be om avklaring av inntektsposter som stammer fra frilansoppdrag
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getFrilansOppdrag();
        if (!yrkesaktiviteter.isEmpty()
            && yrkesaktiviteter.stream().anyMatch(y -> Objects.equals(y.getArbeidsgiver(), inntekt.getArbeidsgiver()))) {
            return false;
        }

        return filter.getYrkesaktiviteter()
            .stream()
            .noneMatch(yr -> ARBEIDSFORHOLD_TYPER.contains(yr.getArbeidType()) && yr.getArbeidsgiver().equals(inntekt.getArbeidsgiver()))
            && filter.getArbeidsforholdOverstyringer()
            .stream()
            .noneMatch(it -> Objects.equals(it.getArbeidsgiver(), inntekt.getArbeidsgiver())
                && Objects.equals(it.getHandling(), ArbeidsforholdHandlingType.IKKE_BRUK));
    }

}
