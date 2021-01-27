package no.nav.k9.sak.domene.arbeidsforhold;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMedÅrsak;
import no.nav.k9.sak.domene.arbeidsforhold.impl.InntektsmeldingVurderingInput;
import no.nav.k9.sak.domene.arbeidsforhold.impl.YtelsespesifikkeInntektsmeldingTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class VurderArbeidsforholdTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<YtelsespesifikkeInntektsmeldingTjeneste> påkrevdeInntektsmeldingerTjenester;

    VurderArbeidsforholdTjeneste() {
        // CDI
    }

    @Inject
    public VurderArbeidsforholdTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                        @Any Instance<YtelsespesifikkeInntektsmeldingTjeneste> påkrevdeInntektsmeldingerTjenester) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
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
     * @param behandlingReferanse behandlingen
     * @return Arbeidsforholdene det må tas stilling til
     */
    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder(BehandlingReferanse behandlingReferanse) {
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingReferanse.getBehandlingId()).orElse(null);
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> arbeidsgiverSetMap = vurderMedÅrsak(behandlingReferanse, inntektArbeidYtelseGrunnlag);
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
     * @param ref      behandlingen
     * @param grunnlag
     * @return Arbeidsforholdene det må tas stilling til
     */
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> vurderMedÅrsak(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag) {

        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();

        var vurderingInput = new InntektsmeldingVurderingInput(ref, grunnlag);
        // Ikke relevant å sjekke permisjonen da dette går til avslag ..
        var ytelsespesifikkeInntektsmeldingTjeneste = finnPåkrevdeInntektsmeldingerTjeneste(ref);
        leggTilManglendePåkrevdeInntektsmeldinger(vurderingInput, result, ytelsespesifikkeInntektsmeldingTjeneste);
        erMottattInntektsmeldingUtenArbeidsforhold(result, vurderingInput, ytelsespesifikkeInntektsmeldingTjeneste);
        erOvergangMedArbeidsforholdsIdHosSammeArbeidsgiver(result, vurderingInput, ytelsespesifikkeInntektsmeldingTjeneste);

        return result;
    }

    private void erOvergangMedArbeidsforholdsIdHosSammeArbeidsgiver(Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result, InntektsmeldingVurderingInput ref, YtelsespesifikkeInntektsmeldingTjeneste ytelsespesifikkeInntektsmeldingTjeneste) {
        var resultMap = ytelsespesifikkeInntektsmeldingTjeneste.erOvergangMedArbeidsforholdsIdHosSammeArbeidsgiver(ref);
        resultMap.forEach((k, v) -> result.merge(k, v, this::mergeSets));
    }

    private void leggTilManglendePåkrevdeInntektsmeldinger(InntektsmeldingVurderingInput ref, Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result, YtelsespesifikkeInntektsmeldingTjeneste ytelsespesifikkeInntektsmeldingTjeneste) {
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
                                                            InntektsmeldingVurderingInput behandlingReferanse,
                                                            YtelsespesifikkeInntektsmeldingTjeneste ytelsespesifikkeInntektsmeldingTjeneste) {

        var resultMap = ytelsespesifikkeInntektsmeldingTjeneste.erMottattInntektsmeldingUtenArbeidsforhold(behandlingReferanse);
        resultMap.forEach((k, v) -> result.merge(k, v, this::mergeSets));
    }

}
