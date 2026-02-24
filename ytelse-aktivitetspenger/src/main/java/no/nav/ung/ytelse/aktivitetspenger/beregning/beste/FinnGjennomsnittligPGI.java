package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FinnGjennomsnittligPGI {

    public record Resultat(
        Map<Year, BigDecimal> pgiPerÅr,
        Map<String, LocalDateTimeline<?>> regelSporingMap
    ) {}

    public static Resultat finnGjennomsnittligPGI(Year sisteTilgjengeligeGSnittÅr, Year grunnbeløpÅr, List<Inntektspost> inntekter) {
        LocalDateTimeline<Grunnbeløp> gsnittTidsserie = GrunnbeløpTidslinje.hentGrunnbeløpSnittTidslinje();
        LocalDateTimeline<BigDecimal> inflasjonsfaktorTidsserie = GrunnbeløpTidslinje.lagInflasjonsfaktorTidslinje(grunnbeløpÅr, 3);
        var årsinntekter = lagÅrsinntektTidslinje(sisteTilgjengeligeGSnittÅr, inntekter);

        var pgiPerÅr = årsinntekter.entrySet().stream()
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

        var regelSporingMap = new java.util.LinkedHashMap<String, LocalDateTimeline<?>>();
        regelSporingMap.put("gsnittTidsserie", gsnittTidsserie);
        regelSporingMap.put("inflasjonsfaktorTidsserie", inflasjonsfaktorTidsserie);
        årsinntekter.forEach((år, tidslinje) -> regelSporingMap.put("årsinntekt_" + år, tidslinje));

        return new Resultat(pgiPerÅr, regelSporingMap);
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

    private static Map<Year, LocalDateTimeline<PgiUtregner>> lagÅrsinntektTidslinje(Year sisteTilgjengeligeLigningsår, List<Inntektspost> inntekter) {
        var sisteTilgjengeligeLigningsårTom = sisteTilgjengeligeLigningsår.atMonth(12).atEndOfMonth();

        return inntekter.stream()
            .filter(ip -> !ip.getPeriode().getTomDato().isAfter(sisteTilgjengeligeLigningsårTom))
            .collect(Collectors.groupingBy(
                ip -> Year.of(ip.getPeriode().getFomDato().getYear()),
                Collectors.reducing(
                    BigDecimal.ZERO,
                    ip -> ip.getBeløp().getVerdi(),
                    BigDecimal::add
                )
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(
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
            if (grunnbeløpSnitt == null) {
                return pgInntekt;
            }

            if (pgInntekt.compareTo(grunnbeløpSnitt(6)) < 1) {
                return pgInntekt;
            } else {
                var avgrensetMot12G = pgInntekt.min(grunnbeløpSnitt(12));
                var pgBidragMellomSeksOgTolvG = (avgrensetMot12G.subtract(grunnbeløpSnitt(6)))
                    .divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_EVEN);

                var gsnitt6 = grunnbeløpSnitt(6);
                return pgBidragMellomSeksOgTolvG.add(gsnitt6);
            }
        }

        private BigDecimal grunnbeløpSnitt(int antall) {
            return grunnbeløpSnitt.multiply(BigDecimal.valueOf(antall));
        }
    }
}
