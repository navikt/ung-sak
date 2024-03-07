package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.folketrygdloven.kalkulus.beregning.v1.KravperioderPrArbeidsforhold;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PerioderForKrav;
import no.nav.folketrygdloven.kalkulus.beregning.v1.Refusjonsperiode;
import no.nav.folketrygdloven.kalkulus.felles.v1.Beløp;
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

        Map<Kravnøkkel, Inntektsmelding> sisteIMPrArbeidsforhold = finnSisteInntektsmeldingMedRefusjonPrArbeidsforhold(vilkårsperiode, imTjeneste, inntektsmeldinger, grunnlagDto.getArbeidDto());
        Map<Kravnøkkel, List<Inntektsmelding>> gruppertPrArbeidsforhold = finnInntektsmeldingMedRefusjonPrArbeidsforhold(inntektsmeldinger, vilkårsperiode, grunnlagDto.getArbeidDto());

        List<KravperioderPrArbeidsforhold> kravPrArbeidsforhold = gruppertPrArbeidsforhold
            .entrySet()
            .stream()
            .filter(e -> sisteIMPrArbeidsforhold.containsKey(e.getKey()))
            .map(e -> mapTilKravPrArbeidsforhold(vilkårsperiode, grunnlagDto, sisteIMPrArbeidsforhold, e))
            .collect(Collectors.toList());
        return kravPrArbeidsforhold.isEmpty() ? null : kravPrArbeidsforhold;
    }

    private static Map<Kravnøkkel, List<Inntektsmelding>> finnInntektsmeldingMedRefusjonPrArbeidsforhold(Collection<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet vilkårsperiode, ArbeidDto arbeidDto) {
        List<Inntektsmelding> inntektsmeldingerMedRefusjonskrav = filtrerKunRefusjon(inntektsmeldinger, vilkårsperiode, arbeidDto);
        return grupper(inntektsmeldingerMedRefusjonskrav);
    }

    private static List<Inntektsmelding> filtrerKunRefusjon(Collection<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet vilkårsperiode, ArbeidDto arbeidDto) {
        return inntektsmeldinger.stream()
            .filter(im -> (im.getRefusjonBeløpPerMnd() != null && !im.getRefusjonBeløpPerMnd().erNullEllerNulltall()) ||
                im.getEndringerRefusjon().stream().anyMatch(e -> !e.getRefusjonsbeløp().erNullEllerNulltall() && vilkårsperiode.inkluderer(e.getFom())))
            .filter(im -> !refusjonOpphørerFørStart(finnStartdatoRefusjon(im, vilkårsperiode.getFomDato(), arbeidDto), finnOpphørRefusjon(im, vilkårsperiode)))
            .collect(Collectors.toList());
    }

    private static Map<Kravnøkkel, Inntektsmelding> finnSisteInntektsmeldingMedRefusjonPrArbeidsforhold(DatoIntervallEntitet vilkårsperiode, InntektsmeldingerRelevantForBeregning imTjeneste, Collection<Inntektsmelding> inntektsmeldinger, ArbeidDto arbeidDto) {
        List<Inntektsmelding> sisteInntektsmeldinger = imTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldinger, vilkårsperiode);
        return grupperEneste(filtrerKunRefusjon(sisteInntektsmeldinger, vilkårsperiode, arbeidDto));
    }

    private static KravperioderPrArbeidsforhold mapTilKravPrArbeidsforhold(DatoIntervallEntitet vilkårsperiode,
                                                                           InntektArbeidYtelseGrunnlagDto grunnlagDto,
                                                                           Map<Kravnøkkel, Inntektsmelding> sisteIMPrArbeidsforhold,
                                                                           Map.Entry<Kravnøkkel, List<Inntektsmelding>> e) {
        List<PerioderForKrav> alleTidligereKravPerioder = lagPerioderForAlle(vilkårsperiode, grunnlagDto, e.getValue());
        PerioderForKrav sistePerioder = lagPerioderForKrav(
            sisteIMPrArbeidsforhold.get(e.getKey()),
            vilkårsperiode,
            grunnlagDto.getArbeidDto());
        return new KravperioderPrArbeidsforhold(
            mapTilAktør(e.getKey().arbeidsgiver),
            mapReferanse(e.getKey().referanse),
            alleTidligereKravPerioder,
            sistePerioder);
    }

    private static List<PerioderForKrav> lagPerioderForAlle(DatoIntervallEntitet vilkårsperiode, InntektArbeidYtelseGrunnlagDto grunnlagDto, List<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .map(im -> lagPerioderForKrav(im, vilkårsperiode, grunnlagDto.getArbeidDto()))
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
                                                      DatoIntervallEntitet vilkårsperiode,
                                                      ArbeidDto arbeidDto) {
        LocalDate startRefusjon = finnStartdatoRefusjon(im, vilkårsperiode.getFomDato(), arbeidDto);
        return new PerioderForKrav(im.getInnsendingstidspunkt().toLocalDate(), mapRefusjonsperioder(im, startRefusjon, vilkårsperiode));
    }

    private static LocalDate finnStartdatoRefusjon(Inntektsmelding im, LocalDate skjæringstidspunktBeregning,
                                                   ArbeidDto arbeidDto) {
        if (arbeidDto == null) {
            throw new IllegalStateException("Krever arbeidDto for mapping av inntektsmelding");
        }
        LocalDate startDatoArbeid = finnStartdatoArbeid(im, skjæringstidspunktBeregning, arbeidDto);
        return startDatoArbeid.isAfter(skjæringstidspunktBeregning) ? startDatoArbeid : skjæringstidspunktBeregning;
    }

    private static LocalDate finnStartdatoArbeid(Inntektsmelding im, LocalDate skjæringstidspunktBeregning, ArbeidDto arbeidDto) {
        return arbeidDto.getYrkesaktiviteter().stream()
            .filter(y -> y.getArbeidsgiver().getIdent().equals(im.getArbeidsgiver().getIdentifikator()) &&
                matcherReferanse(y.getAbakusReferanse(), im.getArbeidsforholdRef()))
            .flatMap(y -> y.getAktivitetsAvtaler().stream())
            .filter(a -> a.getStillingsprosent() == null)
            .map(AktivitetsAvtaleDto::getPeriode)
            .filter(a -> !a.getTom().isBefore(skjæringstidspunktBeregning))
            .map(Periode::getFom)
            .min(Comparator.naturalOrder())
            .orElse(skjæringstidspunktBeregning);
    }

    private static List<Refusjonsperiode> mapRefusjonsperioder(Inntektsmelding im, LocalDate startdatoRefusjon, DatoIntervallEntitet vilkårsperiode) {
        ArrayList<LocalDateSegment<BigDecimal>> alleSegmenter = new ArrayList<>();
        var opphørsdatoRefusjon = finnOpphørRefusjon(im, vilkårsperiode);
        if (refusjonOpphørerFørStart(startdatoRefusjon, opphørsdatoRefusjon)) {
            return Collections.emptyList();
        }
        if (!(im.getRefusjonBeløpPerMnd() == null || im.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) == 0)) {
            alleSegmenter.add(new LocalDateSegment<>(startdatoRefusjon, TIDENES_ENDE, im.getRefusjonBeløpPerMnd().getVerdi()));
        }

        alleSegmenter.addAll(im.getEndringerRefusjon().stream()
            .filter(e -> vilkårsperiode.inkluderer(e.getFom()))
            .map(e ->
                new LocalDateSegment<>(e.getFom(), TIDENES_ENDE, e.getRefusjonsbeløp().getVerdi())
            ).collect(Collectors.toList()));

        var refusjonTidslinje = new LocalDateTimeline<>(alleSegmenter, (interval, lhs, rhs) -> {
            if (lhs.getFom().isBefore(rhs.getFom())) {
                return new LocalDateSegment<>(interval, rhs.getValue());
            }
            return new LocalDateSegment<>(interval, lhs.getValue());
        });
        var vilkårsperiodeTidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(vilkårsperiode.getFomDato(), vilkårsperiode.getTomDato(), true)));
        return refusjonTidslinje.intersection(vilkårsperiodeTidslinje).stream()
            .map(r -> new Refusjonsperiode(new Periode(r.getFom(), r.getTom()), Beløp.fra(r.getValue())))
            .collect(Collectors.toList());

    }

    private static LocalDate finnOpphørRefusjon(Inntektsmelding im, DatoIntervallEntitet vilkårsperiode) {
        return im.getRefusjonOpphører() != null && im.getRefusjonOpphører().isBefore(vilkårsperiode.getTomDato()) ? im.getRefusjonOpphører() : vilkårsperiode.getTomDato();
    }

    private static boolean refusjonOpphørerFørStart(LocalDate startdatoRefusjon, LocalDate refusjonOpphører) {
        return refusjonOpphører != null && refusjonOpphører.isBefore(startdatoRefusjon);
    }

    private static boolean matcherReferanse(InternArbeidsforholdRefDto ref1, InternArbeidsforholdRef ref2) {
        return InternArbeidsforholdRef.ref(ref1 == null ? null : ref1.getAbakusReferanse()).gjelderFor(ref2);
    }

    public static record Kravnøkkel(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef referanse) {
    }
}
