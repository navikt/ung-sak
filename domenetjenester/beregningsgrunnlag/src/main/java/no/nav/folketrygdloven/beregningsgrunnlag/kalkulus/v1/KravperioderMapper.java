package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.folketrygdloven.kalkulus.beregning.v1.KravperioderPrArbeidsforhold;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PerioderForKrav;
import no.nav.folketrygdloven.kalkulus.beregning.v1.Refusjonsperiode;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.AktivitetsAvtaleDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidDto;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper.mapTilAktør;
import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

public class KravperioderMapper {

    public static List<KravperioderPrArbeidsforhold> map(BehandlingReferanse referanse,
                                                         DatoIntervallEntitet vilkårsperiode,
                                                         Collection<Inntektsmelding> sakInntektsmeldinger,
                                                         InntektsmeldingerRelevantForBeregning imTjeneste,
                                                         InntektArbeidYtelseGrunnlagDto grunnlagDto) {
        Collection<Inntektsmelding> inntektsmeldinger = imTjeneste.begrensSakInntektsmeldinger(referanse, sakInntektsmeldinger, vilkårsperiode);

        Map<Kravnøkkel, Inntektsmelding> sisteIMPrArbeidsforhold = finnSisteInntektsmeldingMedRefusjonPrArbeidsforhold(vilkårsperiode, imTjeneste, inntektsmeldinger);
        Map<Kravnøkkel, List<Inntektsmelding>> gruppertPrArbeidsforhold = finnInntektsmeldingMedRefusjonPrArbeidsforhold(inntektsmeldinger);

        List<KravperioderPrArbeidsforhold> kravPrArbeidsforhold = gruppertPrArbeidsforhold
            .entrySet()
            .stream()
            .filter(e -> sisteIMPrArbeidsforhold.containsKey(e.getKey()))
            .map(e -> mapTilKravPrArbeidsforhold(vilkårsperiode, grunnlagDto, sisteIMPrArbeidsforhold, e))
            .collect(Collectors.toList());
        return kravPrArbeidsforhold.isEmpty() ? null : kravPrArbeidsforhold;
    }

    private static Map<Kravnøkkel, List<Inntektsmelding>> finnInntektsmeldingMedRefusjonPrArbeidsforhold(Collection<Inntektsmelding> inntektsmeldinger) {
        List<Inntektsmelding> inntektsmeldingerMedRefusjonskrav = filtrerKunRefusjon(inntektsmeldinger);
        return grupper(inntektsmeldingerMedRefusjonskrav);
    }

    private static List<Inntektsmelding> filtrerKunRefusjon(Collection<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .filter(im -> (im.getRefusjonBeløpPerMnd() != null && !im.getRefusjonBeløpPerMnd().erNullEllerNulltall()) ||
                im.getEndringerRefusjon().stream().anyMatch(e -> !e.getRefusjonsbeløp().erNullEllerNulltall()))
            .collect(Collectors.toList());
    }

    private static Map<Kravnøkkel, Inntektsmelding> finnSisteInntektsmeldingMedRefusjonPrArbeidsforhold(DatoIntervallEntitet vilkårsperiode, InntektsmeldingerRelevantForBeregning imTjeneste, Collection<Inntektsmelding> inntektsmeldinger) {
        List<Inntektsmelding> sisteInntektsmeldinger = imTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldinger, vilkårsperiode);
        return grupperEneste(filtrerKunRefusjon(sisteInntektsmeldinger));
    }

    private static KravperioderPrArbeidsforhold mapTilKravPrArbeidsforhold(DatoIntervallEntitet vilkårsperiode,
                                                                           InntektArbeidYtelseGrunnlagDto grunnlagDto,
                                                                           Map<Kravnøkkel, Inntektsmelding> sisteIMPrArbeidsforhold, Map.Entry<Kravnøkkel, List<Inntektsmelding>> e) {
        List<PerioderForKrav> alleTidligereKravPerioder = lagPerioderForAlle(vilkårsperiode, grunnlagDto, e.getValue());
        PerioderForKrav sistePerioder = lagPerioderForKrav(
            sisteIMPrArbeidsforhold.get(e.getKey()),
            vilkårsperiode.getFomDato(),
            grunnlagDto.getArbeidDto());
        return new KravperioderPrArbeidsforhold(
            mapTilAktør(e.getKey().arbeidsgiver),
            mapReferanse(e.getKey().referanse),
            alleTidligereKravPerioder,
            sistePerioder);
    }

    private static List<PerioderForKrav> lagPerioderForAlle(DatoIntervallEntitet vilkårsperiode, InntektArbeidYtelseGrunnlagDto grunnlagDto, List<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .map(im -> lagPerioderForKrav(im, vilkårsperiode.getFomDato(), grunnlagDto.getArbeidDto()))
            .collect(Collectors.toList());
    }

    private static Map<Kravnøkkel, List<Inntektsmelding>> grupper(List<Inntektsmelding> inntektsmeldingerMedRefusjonskrav) {
        Map<Kravnøkkel, List<Inntektsmelding>> resultMap = inntektsmeldingerMedRefusjonskrav.stream()
            .map(im -> new Kravnøkkel(im.getArbeidsgiver(), im.getArbeidsforholdRef()))
            .distinct()
            .collect(Collectors.toMap(n -> n, n -> new ArrayList<>()));
        inntektsmeldingerMedRefusjonskrav.forEach(im -> {
            var nøkler = finnKeysSomSkalHaInntektsmelding(resultMap, im);
            nøkler.forEach(n -> resultMap.get(n).add(im));
        });
        return resultMap;
    }

    private static Set<Kravnøkkel> finnKeysSomSkalHaInntektsmelding(Map<Kravnøkkel, List<Inntektsmelding>> resultMap, Inntektsmelding im) {
        return resultMap.keySet().stream().filter(n -> n.arbeidsgiver.equals(im.getArbeidsgiver()) && n.referanse.gjelderFor(im.getArbeidsforholdRef())).collect(Collectors.toSet());
    }

    private static Map<Kravnøkkel, Inntektsmelding> grupperEneste(List<Inntektsmelding> inntektsmeldingerMedRefusjonskrav) {
        return inntektsmeldingerMedRefusjonskrav.stream()
            .collect(Collectors.toMap(im ->
                    new Kravnøkkel(im.getArbeidsgiver(), im.getArbeidsforholdRef()),
                im -> im));
    }

    private static InternArbeidsforholdRefDto mapReferanse(InternArbeidsforholdRef arbeidsforholdRef) {
        return arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()
            ? new InternArbeidsforholdRefDto(arbeidsforholdRef.getReferanse())
            : null;
    }

    private static PerioderForKrav lagPerioderForKrav(Inntektsmelding im,
                                                      LocalDate skjæringstidspunktBeregning,
                                                      ArbeidDto arbeidDto) {
        LocalDate startRefusjon = finnStartdatoRefusjon(im, skjæringstidspunktBeregning, arbeidDto);
        return new PerioderForKrav(im.getInnsendingstidspunkt().toLocalDate(), mapRefusjonsperioder(im, startRefusjon));
    }

    private static LocalDate finnStartdatoRefusjon(Inntektsmelding im, LocalDate skjæringstidspunktBeregning,
                                                   ArbeidDto arbeidDto) {
        LocalDate startRefusjon;
        if (arbeidDto != null) {
            LocalDate startDatoArbeid = arbeidDto.getYrkesaktiviteter().stream()
                .filter(y -> y.getArbeidsgiver().getIdent().equals(im.getArbeidsgiver().getIdentifikator()) &&
                    matcherReferanse(y.getAbakusReferanse(), im.getArbeidsforholdRef()))
                .flatMap(y -> y.getAktivitetsAvtaler().stream())
                .filter(a -> a.getStillingsprosent() == null)
                .map(AktivitetsAvtaleDto::getPeriode)
                .filter(a -> !a.getTom().isBefore(skjæringstidspunktBeregning))
                .map(Periode::getFom)
                .min(Comparator.naturalOrder())
                .orElse(skjæringstidspunktBeregning);
            if (startDatoArbeid.isAfter(skjæringstidspunktBeregning)) {
                if (im.getStartDatoPermisjon().isEmpty()) {
                    startRefusjon = startDatoArbeid;
                } else {
                    startRefusjon = startDatoArbeid.isAfter(im.getStartDatoPermisjon().get()) ?
                        startDatoArbeid : im.getStartDatoPermisjon().get();
                }
            } else {
                startRefusjon = skjæringstidspunktBeregning;
            }
        } else {
            startRefusjon = skjæringstidspunktBeregning;
        }
        return startRefusjon;
    }

    private static List<Refusjonsperiode> mapRefusjonsperioder(Inntektsmelding im, LocalDate startdatoRefusjon) {
        ArrayList<LocalDateSegment<BigDecimal>> alleSegmenter = new ArrayList<>();
        if (im.getRefusjonOpphører() != null && im.getRefusjonOpphører().isBefore(startdatoRefusjon)) {
            return Collections.emptyList();
        }
        if (!(im.getRefusjonBeløpPerMnd() == null || im.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) == 0)) {
            alleSegmenter.add(new LocalDateSegment<>(startdatoRefusjon, TIDENES_ENDE, im.getRefusjonBeløpPerMnd().getVerdi()));
        }

        alleSegmenter.addAll(im.getEndringerRefusjon().stream().map(e ->
            new LocalDateSegment<>(e.getFom(), TIDENES_ENDE, e.getRefusjonsbeløp().getVerdi())
        ).collect(Collectors.toList()));

        if (im.getRefusjonOpphører() != null && !im.getRefusjonOpphører().equals(TIDENES_ENDE)) {
            alleSegmenter.add(new LocalDateSegment<>(im.getRefusjonOpphører().plusDays(1), TIDENES_ENDE, BigDecimal.ZERO));
        }

        var refusjonTidslinje = new LocalDateTimeline<>(alleSegmenter, (interval, lhs, rhs) -> {
            if (lhs.getFom().isBefore(rhs.getFom())) {
                return new LocalDateSegment<>(interval, rhs.getValue());
            }
            return new LocalDateSegment<>(interval, lhs.getValue());
        });
        return refusjonTidslinje.stream()
            .map(r -> new Refusjonsperiode(new Periode(r.getFom(), r.getTom()), r.getValue()))
            .collect(Collectors.toList());

    }

    private static boolean matcherReferanse(InternArbeidsforholdRefDto ref1, InternArbeidsforholdRef ref2) {
        return (ref1 == null && ref2 == null)
            || (ref1 != null && ref2 != null && Objects.equals(ref1.getAbakusReferanse(), ref2.getReferanse()));
    }

    public static record Kravnøkkel(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef referanse) { }
}
