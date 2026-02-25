package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FinnGjennomsnittligPGI {

    public static Map<Year, BigDecimal> finnGjennomsnittligPGI(BesteBeregning.BesteBeregningInput input) {
        var gsnittTidsserie = input.gsnittTidsserie();
        var inflasjonsfaktorTidsserie = input.inflasjonsfaktorTidsserie();
        var årsinntekter = opprettTidsseriAvÅrsinntekter(input.årsinntektMap());

        return årsinntekter.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue()
                    .intersection(gsnittTidsserie, leggTilGrunnbeløpSnitt())
                    .intersection(inflasjonsfaktorTidsserie, leggTilInflasjonsfaktor())
                    .mapValue(PgiUtregner::beregnPGI)
                    .stream()
                    .map(LocalDateSegment::getValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
            ));
    }

    private static LocalDateSegmentCombinator<PgiUtregner, Grunnbeløp, PgiUtregner> leggTilGrunnbeløpSnitt() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue();
            return new LocalDateSegment<>(di, builder.setGrunnbeløpSnitt(rhs.getValue().verdi()));
        };
    }

    private static LocalDateSegmentCombinator<PgiUtregner, BigDecimal, PgiUtregner> leggTilInflasjonsfaktor() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue();
            return new LocalDateSegment<>(di, builder.setInflasjonsfaktor(rhs.getValue()));
        };
    }

    private static Map<Year, LocalDateTimeline<PgiUtregner>> opprettTidsseriAvÅrsinntekter(Map<Year, BigDecimal> årsinntekter) {
            return årsinntekter.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    var år = entry.getKey();
                    var segment = new LocalDateSegment<>(
                        new LocalDateInterval(år.atDay(1), år.atMonth(12).atEndOfMonth()),
                        new PgiUtregner(entry.getValue())
                    );
                    return new LocalDateTimeline<>(List.of(segment));
                }
            ));
    }

    static class PgiUtregner {
        BigDecimal pgInntekt;
        BigDecimal grunnbeløpSnitt;
        BigDecimal inflasjonsfaktor;

        public PgiUtregner(BigDecimal pgInntekt) {
            this.pgInntekt = pgInntekt;
        }

        public PgiUtregner setGrunnbeløpSnitt(BigDecimal grunnbeløpSnitt) {
            this.grunnbeløpSnitt = grunnbeløpSnitt;
            return this;
        }

        public PgiUtregner setInflasjonsfaktor(BigDecimal inflasjonsfaktor) {
            this.inflasjonsfaktor = inflasjonsfaktor;
            return this;
        }

        public BigDecimal beregnPGI() {
            return beregnBidragTilPgi().multiply(inflasjonsfaktor);
        }

        private BigDecimal beregnBidragTilPgi() {
            return pgInntekt.min(grunnbeløpSnitt(6));
        }

        private BigDecimal grunnbeløpSnitt(int antall) {
            return grunnbeløpSnitt.multiply(BigDecimal.valueOf(antall));
        }
    }
}
