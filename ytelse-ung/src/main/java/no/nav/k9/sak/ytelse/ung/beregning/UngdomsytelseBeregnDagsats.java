package no.nav.k9.sak.ytelse.ung.beregning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@Dependent
class UngdomsytelseBeregnDagsats {


    public static final int VIRKEDAGER_I_ET_ÅR = 260;
    private LagGrunnbeløpTidslinjeTjeneste lagGrunnbeløpTidslinjeTjeneste;

    @Inject
    public UngdomsytelseBeregnDagsats(LagGrunnbeløpTidslinjeTjeneste lagGrunnbeløpTidslinjeTjeneste) {
        this.lagGrunnbeløpTidslinjeTjeneste = lagGrunnbeløpTidslinjeTjeneste;
    }


    LocalDateTimeline<UngdomsytelseSatser> beregnDagsats(LocalDateTimeline<Boolean> perioder, LocalDate fødselsdato) {
        var grunnbeløpTidslinje = lagGrunnbeløpTidslinjeTjeneste.lagGrunnbeløpTidslinjeForPeriode(perioder);
        var grunnbeløpFaktorTidslinje = LagGrunnbeløpFaktorTidslinje.lagGrunnbeløpFaktorTidslinje(fødselsdato);


        var satsTidslinje = perioder
            .intersection(grunnbeløpFaktorTidslinje, StandardCombinators::rightOnly)
            .mapValue(sats -> new UngdomsytelseSatser(null, null, sats.getGrunnbeløpFaktor(), sats.getSatsType()))
            .intersection(grunnbeløpTidslinje, UngdomsytelseBeregnDagsats::multiplyCombinator);
        return satsTidslinje;
    }

    private static LocalDateSegment<UngdomsytelseSatser> multiplyCombinator(LocalDateInterval di, LocalDateSegment<UngdomsytelseSatser> lhs, LocalDateSegment<BigDecimal> rhs) {
        var grunnbeløp = rhs.getValue();
        var dagsats = lhs.getValue().grunnbeløpFaktor().multiply(grunnbeløp)
            .divide(BigDecimal.valueOf(VIRKEDAGER_I_ET_ÅR), 2, RoundingMode.HALF_UP);
        return new LocalDateSegment<>(di, new UngdomsytelseSatser(dagsats, grunnbeløp, lhs.getValue().grunnbeløpFaktor(), lhs.getValue().satsType()));
    }

}
