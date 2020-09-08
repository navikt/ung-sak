package no.nav.k9.sak.domene.arbeidsforhold;

import static java.util.stream.Collectors.flatMapping;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidType.FORENKLET_OPPGJØRSORDNING;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidType.MARITIMT_ARBEIDSFORHOLD;
import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import no.nav.k9.sak.domene.arbeidsforhold.impl.EndringIArbeidsforholdId;
import no.nav.k9.sak.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.k9.sak.domene.arbeidsforhold.impl.YtelsespesifikkeInntektsmeldingTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.typer.AktørId;
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
     * @param sakInntektsmeldinger                   - alle inntektsmeldinger for saken behandlingen tilhører
     * @param skalTaStillingTilEndringArbeidsforhold skal ta stilling til endring i arbeidsforholdRef i inntektsmeldingen
     * @return Arbeidsforholdene det må tas stilling til
     */
    public Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder(BehandlingReferanse behandlingReferanse,
                                                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                  SakInntektsmeldinger sakInntektsmeldinger,
                                                                  boolean skalTaStillingTilEndringArbeidsforhold) {
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> arbeidsgiverSetMap = vurderMedÅrsak(behandlingReferanse, iayGrunnlag, sakInntektsmeldinger, skalTaStillingTilEndringArbeidsforhold);
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
     * @param skalTaStillingTilEndringArbeidsforhold skal ta stilling til endring i arbeidsforholdRef i inntektsmeldingen
     * @return Arbeidsforholdene det må tas stilling til
     */
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> vurderMedÅrsak(BehandlingReferanse ref,
                                                                         InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                         SakInntektsmeldinger sakInntektsmeldinger,
                                                                         boolean skalTaStillingTilEndringArbeidsforhold) {

        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();

        if (skalTaStillingTilEndringArbeidsforhold) {
            Objects.requireNonNull(sakInntektsmeldinger, "sakInntektsmeldinger");
            vurderOmArbeidsforholdKanGjenkjennes(result, sakInntektsmeldinger, iayGrunnlag, ref);
        }

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

    /**
     * Gir forskjellen i inntektsmeldinger mellom to versjoner av inntektsmeldinger.
     * Benyttes for å markere arbeidsforhold det må tas stilling til å hva saksbehandler skal gjøre.
     *
     * @param behandlingReferanse behandlingen
     * @return Endringene i inntektsmeldinger
     */
    public Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> endringerIInntektsmelding(BehandlingReferanse behandlingReferanse,
                                                                                    InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                                    SakInntektsmeldinger sakInntektsmeldinger) {
        Objects.requireNonNull(iayGrunnlag, "iayGrunnlag");
        Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result = new HashMap<>();
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> yrkesaktiviteterPerArbeidsgiver = mapYrkesaktiviteterPerArbeidsgiver(behandlingReferanse, iayGrunnlag);
        Optional<InntektArbeidYtelseGrunnlag> eksisterendeGrunnlag = hentForrigeVersjonAvInntektsmeldingForBehandling(sakInntektsmeldinger, behandlingReferanse.getBehandlingId());
        Optional<InntektsmeldingAggregat> nyAggregat = iayGrunnlag.getInntektsmeldinger();

        final Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> eksisterende = inntektsmeldingerPerArbeidsgiver(eksisterendeGrunnlag
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger));
        final Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> ny = inntektsmeldingerPerArbeidsgiver(nyAggregat);

        if (!eksisterende.equals(ny)) {
            // Klassifiser endringssjekk
            for (Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsgiverSetEntry : ny.entrySet()) {
                EndringIArbeidsforholdId.vurderMedÅrsak(result, arbeidsgiverSetEntry, eksisterende, iayGrunnlag, yrkesaktiviteterPerArbeidsgiver);
            }
        }
        return result;
    }


    private List<Yrkesaktivitet> getAlleArbeidsforhold(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag) {
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId));

        return filter.getAlleYrkesaktiviteter()
            .stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .distinct()
            .collect(Collectors.toList());
    }

    private void erMottattInntektsmeldingUtenArbeidsforhold(Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result,
                                                            BehandlingReferanse behandlingReferanse,
                                                            YtelsespesifikkeInntektsmeldingTjeneste ytelsespesifikkeInntektsmeldingTjeneste) {

        var resultMap = ytelsespesifikkeInntektsmeldingTjeneste.erMottattInntektsmeldingUtenArbeidsforhold(behandlingReferanse);
        resultMap.forEach((k, v) -> result.merge(k, v, this::mergeSets));
    }

    private void vurderOmArbeidsforholdKanGjenkjennes(Map<Arbeidsgiver,
        Set<ArbeidsforholdMedÅrsak>> result,
                                                      SakInntektsmeldinger sakInntektsmeldinger,
                                                      InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                      BehandlingReferanse behandlingReferanse) {
        Objects.requireNonNull(sakInntektsmeldinger, "sakInntektsmeldinger");
        Objects.requireNonNull(iayGrunnlag, "iayGrunnlag");
        var eksisterendeGrunnlag = hentForrigeVersjonAvInntektsmeldingForBehandling(sakInntektsmeldinger, behandlingReferanse.getId());
        var nyAggregat = iayGrunnlag.getInntektsmeldinger();
        var yrkesaktiviteterPerArbeidsgiver = mapYrkesaktiviteterPerArbeidsgiver(behandlingReferanse, iayGrunnlag);

        final Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> eksisterendeIM = inntektsmeldingerPerArbeidsgiver(eksisterendeGrunnlag
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger));
        final Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> ny = inntektsmeldingerPerArbeidsgiver(nyAggregat);

        if (!eksisterendeIM.isEmpty() && !eksisterendeIM.equals(ny)) {
            // Klassifiser endringssjekk
            for (Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyIM : ny.entrySet()) {
                EndringIArbeidsforholdId.vurderMedÅrsak(result, nyIM, eksisterendeIM, iayGrunnlag, yrkesaktiviteterPerArbeidsgiver);
            }
        }
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> mapYrkesaktiviteterPerArbeidsgiver(BehandlingReferanse behandlingReferanse,
                                                                                               InntektArbeidYtelseGrunnlag grunnlag) {
        List<Yrkesaktivitet> yrkesaktiviteter = getAlleArbeidsforhold(behandlingReferanse.getAktørId(), grunnlag);
        return yrkesaktiviteter.stream()
            .collect(Collectors.groupingBy(Yrkesaktivitet::getArbeidsgiver,
                flatMapping(ya -> Stream.of(ya.getArbeidsforholdRef()), Collectors.toSet())));
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> inntektsmeldingerPerArbeidsgiver(Optional<InntektsmeldingAggregat> inntektsmeldingAggregat) {
        if (inntektsmeldingAggregat.isEmpty()) {
            return Collections.emptyMap();
        }
        return inntektsmeldingAggregat.get()
            .getInntektsmeldingerSomSkalBrukes()
            .stream()
            .collect(Collectors.groupingBy(Inntektsmelding::getArbeidsgiver,
                flatMapping(im -> Stream.of(im.getArbeidsforholdRef()), Collectors.toSet())));
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

    /**
     * Henter ut forrige versjon av inntektsmeldinger
     *
     * @param behandlingId iden til behandlingen
     * @return Liste med inntektsmeldinger {@link Inntektsmelding}
     */
    private Optional<InntektArbeidYtelseGrunnlag> hentForrigeVersjonAvInntektsmeldingForBehandling(SakInntektsmeldinger sakInntektsmeldinger, Long behandlingId) {
        Objects.requireNonNull(sakInntektsmeldinger, "sakInntektsmeldiner");
        // litt rar - returnerer forrige grunnlag som har en inntektsmelding annen enn siste benyttede inntektsmelding
        var grunnlagEksternReferanse = sakInntektsmeldinger.getSisteGrunnlagReferanseDerInntektsmeldingerForskjelligFraNyeste(behandlingId);
        return grunnlagEksternReferanse.flatMap(grunnlagUuid -> sakInntektsmeldinger.finnGrunnlag(behandlingId, grunnlagUuid));
    }

}
