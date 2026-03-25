package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpSnittTidslinje;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PgiKalkulator {

    private final PgiKalkulatorInput input;

    public PgiKalkulator(BeregningInput beregningInput) {
        var gsnittTidsserie = GrunnbeløpSnittTidslinje.hentGrunnbeløpSnittTidslinje();
        var grunnbeløpVedStp = GrunnbeløpTidslinje.hentTidslinje()
            .getSegment(new LocalDateInterval(beregningInput.skjæringstidspunkt(), beregningInput.skjæringstidspunkt()))
            .getValue()
            .verdi();
        var oppjusteringsfaktorTidsserie = GrunnbeløpSnittTidslinje.lagOppjusteringsfaktorTidslinje(Year.of(beregningInput.skjæringstidspunkt().getYear()), grunnbeløpVedStp, 4);
        var årsinntektMap = beregningInput.lagTidslinje();

        input = new PgiKalkulatorInput(årsinntektMap, oppjusteringsfaktorTidsserie, gsnittTidsserie, grunnbeløpVedStp);
    }

    public Map<String, LocalDateTimeline<?>> getRegelSporingsmap() {
        var map = new LinkedHashMap<String, LocalDateTimeline<?>>();
        map.put("gsnittTidsserie", input.gsnittTidsserie().mapValue(Grunnbeløp::verdi));
        map.put("oppjusteringsfaktorTidsserie", input.oppjusteringsfaktorTidsserie());
        map.put("grunnbeløpTidsserie", GrunnbeløpTidslinje.hentTidslinje());
        return map;
    }

    public Map<Year, BigDecimal> avgrensOgOppjusterÅrsinntekter() {
        return hentPeriodisertPgiUtregner()
            .mapValue(PgiØvreGrenseVurderer::avgrensOgOppjusterårsinntekt)
            .toSegments().stream()
            .collect(Collectors.groupingBy(
                segment -> Year.of(segment.getFom().getYear()),
                Collectors.reducing(BigDecimal.ZERO,
                    LocalDateSegment::getValue,
                    BigDecimal::add)
            ));
    }

    public Map<Year, BigDecimal> avgrensÅrsinntekterUtenOppjustering() {
        return hentPeriodisertPgiUtregner()
            .mapValue(PgiØvreGrenseVurderer::avkortÅrsinntektMotSeksG)
            .toSegments().stream()
            .collect(Collectors.groupingBy(
                segment -> Year.of(segment.getFom().getYear()),
                Collectors.reducing(BigDecimal.ZERO, LocalDateSegment::getValue, BigDecimal::add)
            ));
    }

    private LocalDateTimeline<PgiØvreGrenseVurderer> hentPeriodisertPgiUtregner() {
        var gsnittTidsserie = input.gsnittTidsserie();
        var oppjusteringsfaktorTidsserie = input.oppjusteringsfaktorTidsserie();
        var årsinntekter = mapTilPgiUtregner(input.årsinntekt());

        return årsinntekter
            .intersection(gsnittTidsserie, leggTilGrunnbeløpSnitt())
            .intersection(oppjusteringsfaktorTidsserie, leggTilOppjusteringsfaktor());
    }

    private static LocalDateSegmentCombinator<PgiØvreGrenseVurderer, Grunnbeløp, PgiØvreGrenseVurderer> leggTilGrunnbeløpSnitt() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue();
            return new LocalDateSegment<>(di, builder.setGrunnbeløpSnitt(rhs.getValue().verdi()));
        };
    }

    private static LocalDateSegmentCombinator<PgiØvreGrenseVurderer, BigDecimal, PgiØvreGrenseVurderer> leggTilOppjusteringsfaktor() {
        return (di, lhs, rhs) -> {
            var builder = lhs.getValue();
            return new LocalDateSegment<>(di, builder.setoppjusteringsfaktor(rhs.getValue()));
        };
    }

    private static LocalDateTimeline<PgiØvreGrenseVurderer> mapTilPgiUtregner(LocalDateTimeline<Beløp> årsinntekter) {
        return årsinntekter.mapValue(entry -> new PgiØvreGrenseVurderer(entry.getVerdi()));
    }

    public static class PgiØvreGrenseVurderer {
        BigDecimal pgiÅrsinntekt;
        BigDecimal grunnbeløpSnitt;
        BigDecimal oppjusteringsfaktor;

        public PgiØvreGrenseVurderer(BigDecimal pgiÅrsinntekt) {
            this.pgiÅrsinntekt = pgiÅrsinntekt;
        }

        public PgiØvreGrenseVurderer setGrunnbeløpSnitt(BigDecimal grunnbeløpSnitt) {
            this.grunnbeløpSnitt = grunnbeløpSnitt;
            return this;
        }

        public PgiØvreGrenseVurderer setoppjusteringsfaktor(BigDecimal oppjusteringsfaktor) {
            this.oppjusteringsfaktor = oppjusteringsfaktor;
            return this;
        }

        public BigDecimal avgrensOgOppjusterårsinntekt() {
            return avkortÅrsinntektMotSeksG().multiply(oppjusteringsfaktor);
        }

        public BigDecimal avkortÅrsinntektMotSeksG() {
            return pgiÅrsinntekt.min(grunnbeløpSnitt(6));
        }

        private BigDecimal grunnbeløpSnitt(int antall) {
            return grunnbeløpSnitt.multiply(BigDecimal.valueOf(antall));
        }
    }
}
