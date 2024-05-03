package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.DagpengerKilde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class BeregnFerietilleggDagpenger extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {

    public static final String ID = "FP_BR 8.11";
    public static final String BESKRIVELSE = "Beregn ferietillegg for dagpenger.";

    private static final BigDecimal FERIEPENGER_SATS = BigDecimal.valueOf(0.095);

    private static final Logger logger = LoggerFactory.getLogger(BeregnFerietilleggDagpenger.class);

    BeregnFerietilleggDagpenger() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        var tilkjentytelseSegmenter = regelModell.getBeregningsresultatPerioder().stream().map(this::lagDagpengeSegment).toList();
        var tilkjentytelseTidslinje = new LocalDateTimeline<>(tilkjentytelseSegmenter);
        var tidslinjeDagpengerTilkjentYtelse = splittPåÅr(tilkjentytelseTidslinje);
        var segmenterMedDagpengerIAndreSystemer = regelModell.getPerioderMedDagpenger().stream()
            .map(p -> new LocalDateSegment<>(p.fom(), p.tom(), p.kilde())).toList();
        var tidslinjeDagpengerAndreKilder = new LocalDateTimeline<>(segmenterMedDagpengerIAndreSystemer).compress();
        Set<Integer> alleÅrMedDagpener = tidslinjeDagpengerTilkjentYtelse.stream().map(LocalDateSegment::getFom).map(LocalDate::getYear).collect(Collectors.toSet());
        Map<Integer, Boolean> årMotOppfyltVilkårMap = new HashMap<>();
        alleÅrMedDagpener.forEach(år -> {
            erVilkårOppfyltForÅr(Year.of(år), tidslinjeDagpengerTilkjentYtelse, tidslinjeDagpengerAndreKilder);
        });
        tidslinjeDagpengerTilkjentYtelse.toSegments().forEach(segment -> {

        });

        Map<String, Object> regelsporing = new LinkedHashMap<>();

        return beregnet(regelsporing);
    }

    private boolean erVilkårOppfyltForÅr(Year årSomSjekkes,
                                      LocalDateTimeline<Long> tidslinjeTilkjentYtelseDagpenger,
                                      LocalDateTimeline<DagpengerKilde> tidslinjeDagpengerAndreKilder) {

        var førsteDagIÅr = årSomSjekkes.atDay(1);
        var tidslinjeÅr = new LocalDateTimeline<Boolean>(førsteDagIÅr, førsteDagIÅr.with(TemporalAdjusters.lastDayOfYear()), Boolean.TRUE);


        var antallVirkedager = tidslinjeDagpengerAndreKilder.mapValue(v -> Boolean.TRUE).crossJoin(tidslinjeTilkjentYtelseDagpenger.mapValue(v -> Boolean.TRUE), StandardCombinators::alwaysTrueForMatch)
            .intersection(tidslinjeÅr)
            .compress()
            .getLocalDateIntervals()
            .stream()
            .map(p -> Virkedager.beregnAntallVirkedager(p.getFomDato(), p.getTomDato()))
            .reduce(Integer::sum)
            .orElse(0);

        return antallVirkedager > 40;

    }

    private LocalDateTimeline<Long> splittPåÅr(LocalDateTimeline<Long> tilkjentytelseTidslinje) {
        var førsteDagIFørsteÅrMedYtelse = tilkjentytelseTidslinje.getMinLocalDate().withDayOfYear(1);
        return tilkjentytelseTidslinje.splitAtRegular(førsteDagIFørsteÅrMedYtelse, tilkjentytelseTidslinje.getMaxLocalDate(), Period.ofYears(1));
    }

    private LocalDateSegment<Long> lagDagpengeSegment(BeregningsresultatPeriode p) {
        var dagsatsFraDagpengerIPerioden = p.getBeregningsresultatAndelList().stream()
            .filter(a -> Inntektskategori.DAGPENGER.equals(a.getInntektskategori()))
            .map(BeregningsresultatAndel::getDagsats)
            .reduce(Long::sum)
            .orElse(0L);
        return new LocalDateSegment<>(p.getFom(), p.getTom(), dagsatsFraDagpengerIPerioden);
    }



}
