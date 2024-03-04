package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding;

import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.typer.Beløp;

public class LagTidslinjeForRefusjon {

    public static LocalDateTimeline<Beløp> lagRefusjontidslinje(Inntektsmelding im, LocalDate stp) {

        if (im.getRefusjonOpphører() != null && im.getRefusjonOpphører().isBefore(stp)) {
            return new LocalDateTimeline<>(List.of(new LocalDateSegment<>(stp,
                TIDENES_ENDE, Beløp.ZERO)));
        }

        var tidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(stp,
            TIDENES_ENDE,
            im.getRefusjonBeløpPerMnd() == null ? Beløp.ZERO : im.getRefusjonBeløpPerMnd())));

        if (im.getEndringerRefusjon() != null) {
            tidslinje = tidslinje.crossJoin(lagEndringTidslinje(im, stp), StandardCombinators::coalesceRightHandSide);
        }

        if (im.getRefusjonOpphører() != null && im.getRefusjonOpphører().isBefore(TIDENES_ENDE)) {
            tidslinje = tidslinje.crossJoin(new LocalDateTimeline<>(List.of(new LocalDateSegment<>(im.getRefusjonOpphører().plusDays(1), TIDENES_ENDE, Beløp.ZERO))), StandardCombinators::coalesceRightHandSide);
        }
        return tidslinje.compress();
    }

    private static LocalDateTimeline<Beløp> lagEndringTidslinje(Inntektsmelding im, LocalDate stp) {
        return new LocalDateTimeline<>(im.getEndringerRefusjon()
            .stream()
            .filter(e -> !stp.isAfter(e.getFom()))
            .map(e -> new LocalDateSegment<>(e.getFom(), TIDENES_ENDE, e.getRefusjonsbeløp()))
            .sorted(Comparator.naturalOrder())
            .toList(), StandardCombinators::coalesceRightHandSide);
    }


}
