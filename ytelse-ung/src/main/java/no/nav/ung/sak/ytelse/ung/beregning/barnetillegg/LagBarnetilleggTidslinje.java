package no.nav.ung.sak.ytelse.ung.beregning.barnetillegg;

import java.math.BigDecimal;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.behandling.BehandlingReferanse;

@Dependent
public class LagBarnetilleggTidslinje {

    public static final BigDecimal BARNETILLEGG_DAGSATS = BigDecimal.valueOf(36);
    private final LagAntallBarnTidslinje lagAntallBarnTidslinje;

    @Inject
    public LagBarnetilleggTidslinje(LagAntallBarnTidslinje lagAntallBarnTidslinje) {
        this.lagAntallBarnTidslinje = lagAntallBarnTidslinje;
    }

    /** Utleder tidslinje for barnetillegg basert på antall barn i siste dag i måneden før
     *  Barn som fødes og dør i samme måned gir ikke barnetillegg basert på denne logikken.
     * @param behandlingReferanse Behandlingreferanse
     * @return Tidslinje for barnetillegg
     */
    public LocalDateTimeline<Barnetillegg> lagTidslinje(BehandlingReferanse behandlingReferanse) {
        // TODO: Lagre sporing av antall barn, og muligens lagre ned hvilke barn
        var antallBarnTidslinje = lagAntallBarnTidslinje.lagAntallBarnTidslinje(behandlingReferanse);
        return beregnBarnetillegg(antallBarnTidslinje);
    }

    private static LocalDateTimeline<Barnetillegg> beregnBarnetillegg(LocalDateTimeline<Integer> antallBarnTidslinje) {
        List<LocalDateSegment<Integer>> antallBarnMånedsvisSegmenter = antallBarnTidslinje
            .toSegments()
            .stream()
            .filter(s -> s.getLocalDateInterval().contains(s.getFom().with(TemporalAdjusters.lastDayOfMonth()))) // Finner siste segment i hver måned
            .map(s -> new LocalDateSegment<>(s.getFom().plusMonths(1).withDayOfMonth(1), s.getTom(), s.getValue())) // Mapper alle segmenter til å starte første dag i neste måned
            .toList();
        var antallBarnForBarnetilleggTidslinje = new LocalDateTimeline<>(antallBarnMånedsvisSegmenter, (di, lhs, rhs) -> StandardCombinators.sum(di, lhs, rhs));
        return antallBarnForBarnetilleggTidslinje.mapValue(antallBarn -> new Barnetillegg(BigDecimal.valueOf(antallBarn).multiply(BARNETILLEGG_DAGSATS), antallBarn));
    }

    public record Barnetillegg(BigDecimal dagsats, int antallBarn) {
    }

}
