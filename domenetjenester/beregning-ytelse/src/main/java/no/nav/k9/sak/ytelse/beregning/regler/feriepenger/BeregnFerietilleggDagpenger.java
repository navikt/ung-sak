package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.domene.typer.tid.Virkedager;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.MottakerType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.DagpengerKilde;

class BeregnFerietilleggDagpenger extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {

    public static final String ID = "FP_BR 8.11";
    public static final String BESKRIVELSE = "Beregn ferietillegg for dagpenger.";
    private static final BigDecimal FERIEPENGER_SATS = BigDecimal.valueOf(0.095);
    private static final LocalDate FØRSTE_DAG_MED_OPPTJENING_AV_FERIETILLEGG = LocalDate.of(2022,1,1);

    BeregnFerietilleggDagpenger() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        Map<String, Object> regelsporing = new LinkedHashMap<>();
        var tilkjentytelseSegmenter = regelModell.getBeregningsresultatPerioder().stream().map(this::lagDagpengeSegment).toList();
        var tilkjentytelseTidslinje = new LocalDateTimeline<>(tilkjentytelseSegmenter);
        var tidslinjeDagpengerTilkjentYtelse = splittPåÅr(tilkjentytelseTidslinje).intersection(lagTidslinjePeriodeEtterRegelendring());
        var segmenterMedDagpengerIAndreSystemer = regelModell.getPerioderMedDagpenger().stream()
            .filter(p-> p.kilde() != DagpengerKilde.FORELDREPENGER)
            .map(p -> new LocalDateSegment<>(p.fom(), p.tom(), Set.of(p.kilde()))).toList();
        var tidslinjeDagpengerAndreKilder = new LocalDateTimeline<>(segmenterMedDagpengerIAndreSystemer, StandardCombinators::union).compress();
        Set<Integer> alleÅrMedDagpenger = tidslinjeDagpengerTilkjentYtelse.stream().map(LocalDateSegment::getFom).map(LocalDate::getYear).collect(Collectors.toSet());
        alleÅrMedDagpenger.forEach(år -> {
            var erOppfyltVilkårsForFeriepenger = erVilkårOppfyltForÅr(Year.of(år), tidslinjeDagpengerTilkjentYtelse, tidslinjeDagpengerAndreKilder);
            if (erOppfyltVilkårsForFeriepenger) {
                beregnFeriepengerForÅr(tidslinjeDagpengerTilkjentYtelse, Year.of(år), regelsporing);
            }
        });

        return beregnet(regelsporing);
    }

    private LocalDateInterval lagTidslinjePeriodeEtterRegelendring() {
        return new LocalDateInterval(FØRSTE_DAG_MED_OPPTJENING_AV_FERIETILLEGG, Tid.TIDENES_ENDE);
    }

    private void beregnFeriepengerForÅr(LocalDateTimeline<List<BeregningsresultatAndel>> tidslinjeDagpengerTilkjentYtelse, Year år, Map<String, Object> regelsporing) {
        Map<BeregnFeriepenger.FeriepengeNøkkel, BigDecimal> avrundingTilgode = new HashMap<>();
        tidslinjeDagpengerTilkjentYtelse.intersection(lagÅrstidslinje(år)).toSegments()
            .forEach(s ->
            {
                var antallFeriepengerDager = BigDecimal.valueOf(Virkedager.beregnAntallVirkedager(s.getFom(), s.getTom()));
                String periodeNavn = "perioden " + s.getLocalDateInterval();
                regelsporing.put("Antall feriepengedager i " + periodeNavn, antallFeriepengerDager);
                regelsporing.put("Opptjeningsår i " + periodeNavn, år.getValue());
                s.getValue().forEach(andel -> {
                    beregnFeriepengerForAndel(år, regelsporing, andel, antallFeriepengerDager, periodeNavn, avrundingTilgode);

                });
            }
            );
    }

    private static void beregnFeriepengerForAndel(Year år, Map<String, Object> regelsporing,
                                                  BeregningsresultatAndel andel,
                                                  BigDecimal antallFeriepengerDager,
                                                  String periodeNavn,
                                                  Map<BeregnFeriepenger.FeriepengeNøkkel, BigDecimal> avrundingTilgode) {
        String andelId = andel.getArbeidsforhold() != null ? andel.getArbeidsgiverId() : andel.getAktivitetStatus().name();
        var dagsatsFeriepenger = BigDecimal.valueOf(andel.getDagsats()).multiply(FERIEPENGER_SATS);
        var feriepengerForPerioden = antallFeriepengerDager.multiply(dagsatsFeriepenger);
        regelsporing.put("Feriepenger." + andel.getMottakerType() + "." + andelId + " i " + periodeNavn, feriepengerForPerioden);
        var endeligFeriepengerForAndelen = finnFeriepengerMedAvrunding(år, andel, avrundingTilgode, feriepengerForPerioden, regelsporing, andelId, periodeNavn);
        BeregningsresultatFeriepengerPrÅr.builder().medOpptjeningÅr(år.atDay(1))
            .medÅrsbeløp(endeligFeriepengerForAndelen).build(andel);
    }

    private static BigDecimal finnFeriepengerMedAvrunding(Year år,
                                                          BeregningsresultatAndel andel,
                                                          Map<BeregnFeriepenger.FeriepengeNøkkel, BigDecimal> avrundingTilgode,
                                                          BigDecimal feriepengerForPerioden,
                                                          Map<String, Object> regelsporing, String andelId, String periodeNavn) {
        BeregnFeriepenger.FeriepengeNøkkel nøkkel = new BeregnFeriepenger.FeriepengeNøkkel(andel.getMottakerType(), andel.getMottakerType() == MottakerType.BRUKER ? null : andel.getArbeidsgiverId(), år.getValue());
        BigDecimal tidligereAvrunding = avrundingTilgode.getOrDefault(nøkkel, BigDecimal.ZERO);
        BigDecimal endeligFeriepengerForAndelen = feriepengerForPerioden.subtract(tidligereAvrunding).setScale(0, RoundingMode.HALF_UP);
        BigDecimal avrunding = endeligFeriepengerForAndelen.subtract(feriepengerForPerioden);
        avrundingTilgode.put(nøkkel, tidligereAvrunding.add(avrunding));
        if (avrunding.compareTo(BigDecimal.ZERO) != 0) {
            regelsporing.put("Feriepenger.avrunding." + andel.getMottakerType() + "." + andelId + " i " + periodeNavn, avrunding);
        }
        return endeligFeriepengerForAndelen;
    }

    private boolean erVilkårOppfyltForÅr(Year årSomSjekkes,
                                      LocalDateTimeline<List<BeregningsresultatAndel>> tidslinjeTilkjentYtelseDagpenger,
                                      LocalDateTimeline<Set<DagpengerKilde>> tidslinjeDagpengerAndreKilder) {

        var tidslinjeÅr = lagÅrstidslinje(årSomSjekkes);


        var tilkjentYtelseForÅr = tidslinjeTilkjentYtelseDagpenger.intersection(tidslinjeÅr).mapValue(v -> Boolean.TRUE);
        var antallVirkedager = tidslinjeDagpengerAndreKilder.filterValue(it->!it.isEmpty()).mapValue(v -> Boolean.TRUE)
            .crossJoin(tidslinjeTilkjentYtelseDagpenger, StandardCombinators::alwaysTrueForMatch)
            .intersection(new LocalDateInterval(årSomSjekkes.atDay(1), tilkjentYtelseForÅr.getMaxLocalDate()))
            .compress()
            .getLocalDateIntervals()
            .stream()
            .map(p -> Virkedager.beregnAntallVirkedager(p.getFomDato(), p.getTomDato()))
            .reduce(Integer::sum)
            .orElse(0);

        return antallVirkedager > 40;

    }

    private static LocalDateTimeline<Boolean> lagÅrstidslinje(Year årSomSjekkes) {
        var førsteDagIÅr = årSomSjekkes.atDay(1);
        var tidslinjeÅr = new LocalDateTimeline<>(førsteDagIÅr, førsteDagIÅr.with(TemporalAdjusters.lastDayOfYear()), Boolean.TRUE);
        return tidslinjeÅr;
    }

    private <T> LocalDateTimeline<T> splittPåÅr(LocalDateTimeline<T> tilkjentytelseTidslinje) {
        var førsteDagIFørsteÅrMedYtelse = tilkjentytelseTidslinje.getMinLocalDate().withDayOfYear(1);
        return tilkjentytelseTidslinje.splitAtRegular(førsteDagIFørsteÅrMedYtelse, tilkjentytelseTidslinje.getMaxLocalDate(), Period.ofYears(1));
    }

    private LocalDateSegment<List<BeregningsresultatAndel>> lagDagpengeSegment(BeregningsresultatPeriode p) {
        var dagpengeAndeler = p.getBeregningsresultatAndelList().stream()
            .filter(a -> Inntektskategori.DAGPENGER.equals(a.getInntektskategori()))
            .toList();
        return new LocalDateSegment<>(p.getFom(), p.getTom(), dagpengeAndeler);
    }


}
