package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;
import no.nav.ung.sak.typer.Beløp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

public class FinnGjennomsnittligPGI {

    public static LocalDateTimeline<BigDecimal> finnGjennomsnittligPGI(LocalDate sisteTilgjengeligeGSnittÅr, List<Inntektspost> inntekter) {
        LocalDateTimeline<Grunnbeløp> gsnittTidsserie = GrunnbeløpTidslinje.hentGrunnbeløpSnittTidslinje();
        LocalDateTimeline<BigDecimal> inflasjonsfaktorTidsserie = GrunnbeløpTidslinje.lagInflasjonsfaktorTidslinje(Year.of(sisteTilgjengeligeGSnittÅr.getYear()),1);

        LocalDateTimeline<PgiUtregner> inntektTidsserie = lagInntektstidslinjeOgOpprettPgiUtregner(inntekter);
        return inntektTidsserie
            .intersection(gsnittTidsserie, leggTilGrunnbeløpSnitt())
            .intersection(inflasjonsfaktorTidsserie, leggTilInflasjonsfaktor())
            .mapValue(PgiUtregner::beregnPGI);
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

    private static LocalDateTimeline<PgiUtregner> lagInntektstidslinjeOgOpprettPgiUtregner(List<Inntektspost> inntekter) {
        var inntektsegmenter = inntekter.stream()
            .filter(ip -> !InntektspostType.YTELSE.equals(ip.getInntektspostType()))
            .map(inntektspost -> {
                var interval = new LocalDateInterval(inntektspost.getPeriode().getFomDato(), inntektspost.getPeriode().getTomDato());
                return new LocalDateSegment<>(interval, new PgiUtregner(inntektspost.getBeløp()));
            })
            .sorted().toList();

        return new LocalDateTimeline<>(inntektsegmenter);
    }

    static class PgiUtregner {
        BigDecimal pgInntekt;
        BigDecimal grunnbeløpSnitt;
        BigDecimal inflasjonsfaktor;

        public PgiUtregner(Beløp pgInntekt) {
            this.pgInntekt = pgInntekt.getVerdi();
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
            if (pgInntekt.compareTo(grunnbeløpSnitt(6)) < 1) {
                return pgInntekt;
            } else {
                var pgBidragMellomSeksOgTolvG = (pgInntekt.min(grunnbeløpSnitt(12)).subtract(grunnbeløpSnitt(6)))
                    .divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_EVEN);

                return pgBidragMellomSeksOgTolvG.add(grunnbeløpSnitt(6));
            }
        }

        private BigDecimal grunnbeløpSnitt(int antall) {
            return grunnbeløpSnitt.multiply(BigDecimal.valueOf(antall));
        }
    }
}
