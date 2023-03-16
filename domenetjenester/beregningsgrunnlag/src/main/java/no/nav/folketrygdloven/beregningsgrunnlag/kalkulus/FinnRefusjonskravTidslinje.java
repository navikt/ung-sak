package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

/**
 * Finner summert refusjonskravtidslinje. Skal normalt ikke brukes til å bestemme eksakt refusjonskrav i automatisk saksbehandling,
 * men kan brukes til visning.
 * <p>
 * NB: Logikken brukes for migreringer fra infotrygd, da dette er spesielle saker som krever sammenstilling av data før det sendes til kalkulus.
 */
class FinnRefusjonskravTidslinje {


    static LocalDateTimeline<BigDecimal> lagTidslinje(LocalDate stp, LocalDateTimeline<Set<Inntektsmelding>> inntektsmeldingerForAktivitet) {
        return inntektsmeldingerForAktivitet
            .map(ims -> ims.getValue().stream()
                .map(im -> tilRefusjontidslinje(im, stp))
                .reduce((t1, t2) -> t1.crossJoin(t2, StandardCombinators::sum))
                .orElse(LocalDateTimeline.empty())
                .intersection(ims.getLocalDateInterval())
                .stream()
                .toList())
            .compress();
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
        if (opphørsdatoRefusjon != null && opphørsdatoRefusjon.isBefore(TIDENES_ENDE)) {
            alleSegmenter.add(new LocalDateSegment<>(opphørsdatoRefusjon.plusDays(1), TIDENES_ENDE, BigDecimal.ZERO));
        }

        // Endringer i mellom
        alleSegmenter.addAll(im.getEndringerRefusjon().stream()
            .map(e ->
                new LocalDateSegment<>(e.getFom(), TIDENES_ENDE, e.getRefusjonsbeløp().getVerdi())
            ).toList());

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
