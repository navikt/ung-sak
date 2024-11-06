package no.nav.k9.sak.ytelse.ung.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.ytelse.ung.beregning.barnetillegg.LagBarnetilleggTidslinje;

@Dependent
public class UngdomsytelseBeregnDagsats {

    private final LagGrunnbeløpTidslinjeTjeneste lagGrunnbeløpTidslinjeTjeneste;
    private final LagBarnetilleggTidslinje lagBarnetilleggTidslinje;

    @Inject
    public UngdomsytelseBeregnDagsats(LagGrunnbeløpTidslinjeTjeneste lagGrunnbeløpTidslinjeTjeneste,
                                      LagBarnetilleggTidslinje lagBarnetilleggTidslinje) {
        this.lagGrunnbeløpTidslinjeTjeneste = lagGrunnbeløpTidslinjeTjeneste;
        this.lagBarnetilleggTidslinje = lagBarnetilleggTidslinje;
    }


    LocalDateTimeline<UngdomsytelseSatser> beregnDagsats(BehandlingReferanse behandlingRef, LocalDateTimeline<Boolean> perioder, LocalDate fødselsdato) {
        var grunnbeløpTidslinje = lagGrunnbeløpTidslinjeTjeneste.lagGrunnbeløpTidslinjeForPeriode(perioder);
        var grunnbeløpFaktorTidslinje = LagGrunnbeløpFaktorTidslinje.lagGrunnbeløpFaktorTidslinje(fødselsdato);
        var barnetilleggTidslinje = lagBarnetilleggTidslinje.lagTidslinje(behandlingRef);

        var satsTidslinje = perioder
            .intersection(grunnbeløpFaktorTidslinje, StandardCombinators::rightOnly)
            .mapValue(UngdomsytelseBeregnDagsats::leggTilSatsTypeOgGrunnbeløpFaktor)
            .intersection(grunnbeløpTidslinje, leggTilGrunnbeløp())
            .combine(barnetilleggTidslinje, leggTilBarnetillegg(), LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .mapValue(UngdomsytelseSatser.Builder::build);
        return satsTidslinje;
    }

    private static UngdomsytelseSatser.Builder leggTilSatsTypeOgGrunnbeløpFaktor(Sats sats) {
        return UngdomsytelseSatser.builder().medGrunnbeløpFaktor(sats.getGrunnbeløpFaktor()).medSatstype(sats.getSatsType());
    }

    private static LocalDateSegmentCombinator<UngdomsytelseSatser.Builder, BigDecimal, UngdomsytelseSatser.Builder> leggTilGrunnbeløp() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue().kopi();
            return new LocalDateSegment<>(di, builder.medGrunnbeløp(rhs.getValue()));
        };
    }

    private static LocalDateSegmentCombinator<UngdomsytelseSatser.Builder, LagBarnetilleggTidslinje.Barnetillegg, UngdomsytelseSatser.Builder> leggTilBarnetillegg() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue().kopi();
            return new LocalDateSegment<>(di,
                rhs == null ?
                    builder.medAntallBarn(0).medBarnetilleggDagsats(BigDecimal.ZERO) :
                    builder.medAntallBarn(rhs.getValue().antallBarn()).medBarnetilleggDagsats(rhs.getValue().dagsats()));
        };
    }

}
