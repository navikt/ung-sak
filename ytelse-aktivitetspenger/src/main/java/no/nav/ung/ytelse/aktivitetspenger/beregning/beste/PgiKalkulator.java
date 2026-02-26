package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Map;
import java.util.stream.Collectors;

public class PgiKalkulator {

    public static PgiKalkulatorInput lagPgiKalkulatorInput(BeregningInput beregningInput) {
        var gsnittTidsserie = GrunnbeløpTidslinje.hentGrunnbeløpSnittTidslinje();
        var oppjusteringsfaktorTidsserie = GrunnbeløpTidslinje.lagOppjusteringsfaktorTidslinje(Year.of(beregningInput.virkningsdato().getYear()), 3);
        var årsinntektMap = beregningInput.lagTidslinje();

        return new PgiKalkulatorInput(årsinntektMap, oppjusteringsfaktorTidsserie, gsnittTidsserie);
    }

    public static LocalDateTimeline<PgiUtregner> hentPeriodisertPgiUtregner(PgiKalkulatorInput input) {
        var gsnittTidsserie = input.gsnittTidsserie();
        var oppjusteringsfaktorTidsserie = input.oppjusteringsfaktorTidsserie();
        var årsinntekter = mapTilPgiUtregner(input.årsinntekt());

        return årsinntekter
                    .intersection(gsnittTidsserie, leggTilGrunnbeløpSnitt())
                    .intersection(oppjusteringsfaktorTidsserie, leggTilOppjusteringsfaktor());
    }

    public static Map<Year, BigDecimal> avgrensOgOppjusterÅrsinntekter(PgiKalkulatorInput input) {
        return hentPeriodisertPgiUtregner(input)
            .mapValue(PgiUtregner::avgrensOgOppjusterårsinntekt)
            .toSegments().stream()
            .collect(Collectors.groupingBy(
                segment -> Year.of(segment.getFom().getYear()),
                Collectors.reducing(
                    BigDecimal.ZERO,
                    LocalDateSegment::getValue,
                    BigDecimal::add
                )
            ));
    }

    private static LocalDateSegmentCombinator<PgiUtregner, Grunnbeløp, PgiUtregner> leggTilGrunnbeløpSnitt() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue();
            return new LocalDateSegment<>(di, builder.setGrunnbeløpSnitt(rhs.getValue().verdi()));
        };
    }

    private static LocalDateSegmentCombinator<PgiUtregner, BigDecimal, PgiUtregner> leggTilOppjusteringsfaktor() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue();
            return new LocalDateSegment<>(di, builder.setoppjusteringsfaktor(rhs.getValue()));
        };
    }

    private static LocalDateTimeline<PgiUtregner> mapTilPgiUtregner(LocalDateTimeline<Beløp> årsinntekter) {
            return årsinntekter.mapValue(entry -> new PgiUtregner(entry.getVerdi()));
    }

    public static class PgiUtregner {
        BigDecimal pgiÅrsinntekt;
        BigDecimal grunnbeløpSnitt;
        BigDecimal oppjusteringsfaktor;

        public PgiUtregner(BigDecimal pgiÅrsinntekt) {
            this.pgiÅrsinntekt = pgiÅrsinntekt;
        }

        public PgiUtregner setGrunnbeløpSnitt(BigDecimal grunnbeløpSnitt) {
            this.grunnbeløpSnitt = grunnbeløpSnitt;
            return this;
        }

        public PgiUtregner setoppjusteringsfaktor(BigDecimal oppjusteringsfaktor) {
            this.oppjusteringsfaktor = oppjusteringsfaktor;
            return this;
        }

        public BigDecimal avgrensOgOppjusterårsinntekt() {
            return beregnBidragTilPgi().multiply(oppjusteringsfaktor);
        }

        private BigDecimal beregnBidragTilPgi() {
            return pgiÅrsinntekt.min(grunnbeløpSnitt(6));
        }

        private BigDecimal grunnbeløpSnitt(int antall) {
            return grunnbeløpSnitt.multiply(BigDecimal.valueOf(antall));
        }
    }
}
