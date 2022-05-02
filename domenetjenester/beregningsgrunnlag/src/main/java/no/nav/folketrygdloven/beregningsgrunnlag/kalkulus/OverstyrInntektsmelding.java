package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.domene.iay.modell.Refusjon;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;

class OverstyrInntektsmelding {

    private OverstyrInntektsmelding() {
    }

    static Set<Inntektsmelding> finnOverstyrteInntektsmeldinger(List<BeregnInput> beregnInput, Set<Inntektsmelding> inntektsmeldingerForSak) {
        return beregnInput.stream().flatMap(i -> i.getInputOverstyringPeriode().stream())
            .flatMap(overstyrtPeriode -> {
                LocalDate stp = overstyrtPeriode.getSkjæringstidspunkt();
                return overstyrtPeriode.getAktivitetOverstyringer().stream()
                    .filter(a -> a.getAktivitetStatus().erArbeidstaker())
                    .map(a -> mapAktivitetTilInntektsmelding(a, stp, inntektsmeldingerForSak));
            }).collect(Collectors.toSet());
    }

    static Inntektsmelding mapAktivitetTilInntektsmelding(InputAktivitetOverstyring a,
                                                          LocalDate stp,
                                                          Set<Inntektsmelding> inntektsmeldingerForSak) {
        var inntektsmeldingerForAktivitet = finnInntektsmeldingerMottattForAktivitet(inntektsmeldingerForSak, stp, a);
        var summertRefusjonTidslinje = lagSummertRefusjontidslinje(stp, inntektsmeldingerForAktivitet);
        return mapInntektsmelding(stp, a, summertRefusjonTidslinje);
    }

    private static Set<Inntektsmelding> finnInntektsmeldingerMottattForAktivitet(Set<Inntektsmelding> inntektsmeldingerForSak, LocalDate stp, InputAktivitetOverstyring a) {
        return inntektsmeldingerForSak.stream().filter(im -> (im.getStartDatoPermisjon().isPresent() && im.getStartDatoPermisjon().get().equals(stp))
                && im.getArbeidsgiver().equals(a.getArbeidsgiver()))
            .collect(Collectors.toSet());
    }

    private static LocalDateTimeline<BigDecimal> lagSummertRefusjontidslinje(LocalDate stp, Set<Inntektsmelding> inntektsmeldingerForAktivitet) {
        return inntektsmeldingerForAktivitet.stream()
            .map(im -> tilRefusjontidslinje(im, stp))
            .reduce((tidslinje1, tidslinje2) -> tidslinje1.combine(tidslinje2, StandardCombinators::sum, LocalDateTimeline.JoinStyle.CROSS_JOIN))
            .orElse(LocalDateTimeline.empty())
            .compress();
    }


    private static Inntektsmelding mapInntektsmelding(LocalDate stp, InputAktivitetOverstyring a, LocalDateTimeline<BigDecimal> summertRefusjonTidslinje) {
        var opphører = finnOpphør(a, summertRefusjonTidslinje);
        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medInnsendingstidspunkt(stp.atStartOfDay())
            .medArbeidsgiver(a.getArbeidsgiver())
            .medStartDatoPermisjon(stp)
            .medRefusjon(finnRefusjonVedStp(stp, summertRefusjonTidslinje, a), opphører)
            .medBeløp(a.getInntektPrÅr().getVerdi().divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medJournalpostId("OVERSTYRT_FOR_INFOTRYGDMIGRERING" + stp)
            .medKanalreferanse("OVERSTYRT_FOR_INFOTRYGDMIGRERING" + stp);
        mapEndringer(stp, summertRefusjonTidslinje, opphører, inntektsmeldingBuilder);
        return inntektsmeldingBuilder
            .build();
    }

    private static void mapEndringer(LocalDate stp, LocalDateTimeline<BigDecimal> summertRefusjonTidslinje, LocalDate opphører, InntektsmeldingBuilder inntektsmeldingBuilder) {
        summertRefusjonTidslinje.toSegments()
            .stream()
            .filter(di -> di.getFom().isAfter(stp) && !di.getLocalDateInterval().overlaps(new LocalDateInterval(stp, stp)) && (opphører == null || di.getFom().isBefore(opphører)))
            .forEach(s -> inntektsmeldingBuilder.leggTil(new Refusjon(s.getValue(), s.getFom())));
    }

    private static LocalDate finnOpphør(InputAktivitetOverstyring a, LocalDateTimeline<BigDecimal> summertRefusjonTidslinje) {
        if (summertRefusjonTidslinje.isEmpty()) {
            return a.getOpphørRefusjon();
        }
        var opphørFraInntektsmelding = summertRefusjonTidslinje.toSegments().stream()
            .filter(s -> s.getTom().equals(TIDENES_ENDE) && s.getValue().compareTo(BigDecimal.ZERO) == 0)
            .findFirst()
            .map(LocalDateSegment::getFom);
        return opphørFraInntektsmelding.orElse(null);
    }

    private static BigDecimal finnRefusjonVedStp(LocalDate stp, LocalDateTimeline<BigDecimal> summertRefusjonTidslinje, InputAktivitetOverstyring a) {
        var stpOverlapp = summertRefusjonTidslinje.getSegment(new LocalDateInterval(stp, stp));
        if (stpOverlapp == null) {
            return a.getRefusjonPrÅr() == null ? BigDecimal.ZERO :
                a.getRefusjonPrÅr().getVerdi().divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
        }
        return stpOverlapp.getValue();
    }

    private static LocalDateTimeline<BigDecimal> tilRefusjontidslinje(Inntektsmelding im, LocalDate startdatoRefusjon) {
        ArrayList<LocalDateSegment<BigDecimal>> alleSegmenter = new ArrayList<>();
        var opphørsdatoRefusjon = im.getRefusjonOpphører();
        if (refusjonOpphørerFørStart(startdatoRefusjon, opphørsdatoRefusjon)) {
            return LocalDateTimeline.empty();
        }
        // Refusjon fra start
        if (!(im.getRefusjonBeløpPerMnd() == null || im.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) == 0)) {
            alleSegmenter.add(new LocalDateSegment<>(startdatoRefusjon, TIDENES_ENDE, im.getRefusjonBeløpPerMnd().getVerdi()));
        }

        // Opphør
        alleSegmenter.add(new LocalDateSegment<>(opphørsdatoRefusjon, TIDENES_ENDE, BigDecimal.ZERO));

        // Endringer i mellom
        alleSegmenter.addAll(im.getEndringerRefusjon().stream()
            .map(e ->
                new LocalDateSegment<>(e.getFom(), TIDENES_ENDE, e.getRefusjonsbeløp().getVerdi())
            ).collect(Collectors.toList()));

        return new LocalDateTimeline<>(alleSegmenter, (interval, lhs, rhs) -> {
            if (lhs.getFom().isBefore(rhs.getFom())) {
                return new LocalDateSegment<>(interval, rhs.getValue());
            }
            return new LocalDateSegment<>(interval, lhs.getValue());
        });

    }

    private static boolean refusjonOpphørerFørStart(LocalDate startdatoRefusjon, LocalDate refusjonOpphører) {
        return refusjonOpphører != null && refusjonOpphører.isBefore(startdatoRefusjon);
    }


}
