package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Map;
import java.util.stream.Collectors;

public class PgiKalkulator {

    public static Map<Year, BigDecimal> avgrensOgOppjusterÅrsinntekter(BeregningInput input) {
        var gsnittTidsserie = input.gsnittTidsserie();
        var inflasjonsfaktorTidsserie = input.inflasjonsfaktorTidsserie();
        var årsinntekter = opprettTidsseriAvÅrsinntekter(input.årsinntektMap());

        return årsinntekter
                    .intersection(gsnittTidsserie, leggTilGrunnbeløpSnitt())
                    .intersection(inflasjonsfaktorTidsserie, leggTilInflasjonsfaktor())
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

    private static LocalDateSegmentCombinator<PgiUtregner, BigDecimal, PgiUtregner> leggTilInflasjonsfaktor() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue();
            return new LocalDateSegment<>(di, builder.setInflasjonsfaktor(rhs.getValue()));
        };
    }

    private static LocalDateTimeline<PgiUtregner> opprettTidsseriAvÅrsinntekter(LocalDateTimeline<Beløp> årsinntekter) {
            return årsinntekter.mapValue(entry -> new PgiUtregner(entry.getVerdi()));
    }


    static class PgiUtregner {
        BigDecimal pgiÅrsinntekt;
        BigDecimal grunnbeløpSnitt;
        BigDecimal inflasjonsfaktor;

        public PgiUtregner(BigDecimal pgiÅrsinntekt) {
            this.pgiÅrsinntekt = pgiÅrsinntekt;
        }

        public PgiUtregner setGrunnbeløpSnitt(BigDecimal grunnbeløpSnitt) {
            this.grunnbeløpSnitt = grunnbeløpSnitt;
            return this;
        }

        public PgiUtregner setInflasjonsfaktor(BigDecimal inflasjonsfaktor) {
            this.inflasjonsfaktor = inflasjonsfaktor;
            return this;
        }

        public BigDecimal avgrensOgOppjusterårsinntekt() {
            return beregnBidragTilPgi().multiply(inflasjonsfaktor);
        }

        private BigDecimal beregnBidragTilPgi() {
            return pgiÅrsinntekt.min(grunnbeløpSnitt(6));
        }

        private BigDecimal grunnbeløpSnitt(int antall) {
            return grunnbeløpSnitt.multiply(BigDecimal.valueOf(antall));
        }
    }
}
