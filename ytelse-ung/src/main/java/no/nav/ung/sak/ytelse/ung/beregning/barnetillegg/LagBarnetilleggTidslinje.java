package no.nav.ung.sak.ytelse.ung.beregning.barnetillegg;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

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

    /**
     * Utleder tidslinje for barnetillegg basert på antall barn i siste dag i måneden før
     * Barn som fødes og dør i samme måned gir ikke barnetillegg basert på denne logikken.
     *
     * @param behandlingReferanse Behandlingreferanse
     * @param perioder
     * @return Tidslinje for barnetillegg
     */
    public BarnetilleggVurdering lagTidslinje(BehandlingReferanse behandlingReferanse, LocalDateTimeline<Boolean> perioder) {
        var barnetilleggMellomregning = lagAntallBarnTidslinje.lagAntallBarnTidslinje(behandlingReferanse);
        return new BarnetilleggVurdering(beregnBarnetillegg(barnetilleggMellomregning.antallBarnTidslinje()), barnetilleggMellomregning.barnFødselOgDødInfo()) ;
    }

    static LocalDateTimeline<Barnetillegg> beregnBarnetillegg(LocalDateTimeline<Integer> antallBarnTidslinje) {
        List<LocalDateSegment<Integer>> antallBarnMånedsvisSegmenter = antallBarnTidslinje
            .toSegments()
            .stream()
            .map(s -> new LocalDateSegment<>(s.getFom().plusMonths(1).withDayOfMonth(1), s.getFom().plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()), s.getValue())) // Mapper alle segmenter til å starte første dag i neste måned
            .toList();
        var antallBarnForBarnetilleggTidslinje = new LocalDateTimeline<>(antallBarnMånedsvisSegmenter, StandardCombinators::max);
        return antallBarnForBarnetilleggTidslinje.mapValue(antallBarn -> new Barnetillegg(finnUtregnetBarnetillegg(antallBarn), antallBarn));
    }

    private static int finnUtregnetBarnetillegg(Integer antallBarn) {
        return BigDecimal.valueOf(antallBarn).multiply(BARNETILLEGG_DAGSATS).intValue(); // Både sats og antall barn er heltall, så vi trenger ingen avrunding. Caster direkte til long
    }

}
