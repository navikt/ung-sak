package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.IntervallUtil;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.MottakerType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

class BeregnFeriepengerV2 extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    private static final Logger logger = LoggerFactory.getLogger(BeregnFeriepengerV2.class);
    public static final String ID = "FP_BR 8.6";
    public static final String BESKRIVELSE = "Beregn feriepenger for periode som går over flere kalenderår.";

    private static final BigDecimal FERIEPENGER_SATS_PROSENT = BigDecimal.valueOf(0.102);

    BeregnFeriepengerV2() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        Map<String, Object> regelsporing = new LinkedHashMap<>();
        LocalDateTimeline<Boolean> tidslinjeDenneSakenHarAndelSomKanGiFeriepenger = FeriepengerForAndelUtil.utledTidslinjerHarAndelSomKanGiFeriepenger(regelModell.getBeregningsresultatPerioder());

        LocalDateTimeline<Boolean> tidslinjeYtelseKanGiFeriepenger = tidslinjeDenneSakenHarAndelSomKanGiFeriepenger.union(regelModell.getAndelerSomKanGiFeriepengerForRelevaneSaker(), StandardCombinators::alwaysTrueForMatch)
            .compress();

        LocalDateTimeline<Boolean> tidslinjeFeriepengerInnenforKvote = utledPerioderInnenforKvote(tidslinjeYtelseKanGiFeriepenger, regelModell);
        LocalDateTimeline<Boolean> tidslinjeFeriepengerInnenforKvoteRefusjon = regelModell.harUbegrensetFeriepengedagerVedRefusjon() ? tidslinjeYtelseKanGiFeriepenger : tidslinjeFeriepengerInnenforKvote;

        regelsporingTidslinjerYtelseKanGiFeriepenger(regelsporing, tidslinjeYtelseKanGiFeriepenger);
        regelsporingTidslinjerInnenforKvote(regelsporing, regelModell, tidslinjeFeriepengerInnenforKvote);

        beregn(regelsporing, regelModell, tidslinjeFeriepengerInnenforKvote, MottakerType.BRUKER);
        beregn(regelsporing, regelModell, tidslinjeFeriepengerInnenforKvoteRefusjon, MottakerType.ARBEIDSGIVER);

        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> andreSakerOverKvote = regelModell.getAndelerSomKanGiFeriepengerForRelevaneSaker().disjoint(tidslinjeFeriepengerInnenforKvote);
        if (!andreSakerOverKvote.isEmpty()) {
            //TODO automatisk revurdere de andre sakene istedet
            logger.warn("Feriepengutregning i denne saken viser at følgende saker nå er over kvote på feriepenger, og bør revurderes: {}", andreSakerOverKvote.stream().flatMap(segment -> segment.getValue().stream()).map(segment -> segment.saksnummer()).collect(Collectors.toSet()));
        }
        return beregnet(regelsporing);
    }

    private static void regelsporingTidslinjerYtelseKanGiFeriepenger(Map<String, Object> regelsporing, LocalDateTimeline<Boolean> tidslinjeYtelseKanGiFeriepenger) {
        regelsporing.put("perioder-med-andeler-som-kan-gi-feriepenger", prettyPrint(tidslinjeYtelseKanGiFeriepenger));
    }

    private static void regelsporingTidslinjerInnenforKvote(Map<String, Object> regelsporing, BeregningsresultatFeriepengerRegelModell regelModell, LocalDateTimeline<Boolean> tidslinjeFeriepengerInnenforKvote) {
        regelsporing.put("perioder-med-andeler-som-kan-gi-feriepenger-og-er-innenfor-kvote", prettyPrint(tidslinjeFeriepengerInnenforKvote));
        if (regelModell.harUbegrensetFeriepengedagerVedRefusjon()) {
            regelsporing.put("perioder-med-andeler-som-kan-gi-feriepenger-og-er-innenfor-kvote", prettyPrint(tidslinjeFeriepengerInnenforKvote));
        }
    }

    private static void beregn(Map<String, Object> regelsporing, BeregningsresultatFeriepengerRegelModell regelModell, LocalDateTimeline<Boolean> tidslinjeHvorFeriepengerGis, MottakerType mottakerType) {
        List<BeregningsresultatPeriode> beregningsresultatPerioder = regelModell.getBeregningsresultatPerioder();
        boolean harFeriepengeopptjeningForHelg = regelModell.harFeriepengeopptjeningForHelg();
        Predicate<BeregningsresultatAndel> andelFilter = andel -> andel.girRettTilFeriepenger() && andel.getMottakerType() == mottakerType;

        for (BeregningsresultatPeriode periode : beregningsresultatPerioder) {
            LocalDateTimeline<Boolean> overlapp = tidslinjeHvorFeriepengerGis.intersection(periode.getPeriode());
            if (!overlapp.isEmpty() && periode.getBeregningsresultatAndelList().stream().anyMatch(andelFilter)) {
                for (LocalDateInterval åretsOverlapp : IntervallUtil.periodiserPrÅr(unikPeriode(overlapp))) {
                    long antallFeriepengerDager = harFeriepengeopptjeningForHelg
                        ? IntervallUtil.beregnKalanderdager(åretsOverlapp)
                        : IntervallUtil.beregnUkedager(åretsOverlapp);

                    String periodeNavn = "perioden " + åretsOverlapp + "for " + mottakerType.name().toLowerCase();
                    regelsporing.put("Antall feriepengedager i " + periodeNavn, antallFeriepengerDager);
                    regelsporing.put("Opptjeningsår i " + periodeNavn, åretsOverlapp.getFomDato().getYear());

                    for (BeregningsresultatAndel andel : periode.getBeregningsresultatAndelList()) {
                        long feriepengerGrunnlag = andel.getDagsats() * antallFeriepengerDager;
                        BigDecimal feriepengerAndelPrÅr = BigDecimal.valueOf(feriepengerGrunnlag).multiply(FERIEPENGER_SATS_PROSENT);
                        if (andelFilter.test(andel) && feriepengerAndelPrÅr.compareTo(BigDecimal.ZERO) != 0) {
                            BeregningsresultatFeriepengerPrÅr.builder()
                                .medOpptjeningÅr(åretsOverlapp.getFomDato().withMonth(12).withDayOfMonth(31))
                                .medÅrsbeløp(feriepengerAndelPrÅr)
                                .build(andel);
                            String andelId = andel.getArbeidsforhold() != null ? andel.getArbeidsgiverId() : andel.getAktivitetStatus().name();
                            regelsporing.put("Feriepenger." + andel.getMottakerType() + "." + andelId + " i " + periodeNavn, feriepengerAndelPrÅr);
                        }
                    }
                }
            }
        }
    }

    private static LocalDateInterval unikPeriode(LocalDateTimeline<?> tidslinje) {
        var perioder = tidslinje.getLocalDateIntervals();
        if (perioder.size() == 1) {
            return perioder.iterator().next();
        }
        throw new IllegalArgumentException("Forventet tidslinje med nøyaktig en periode, men fikk: " + tidslinje);
    }

    private static String prettyPrint(LocalDateTimeline<Boolean> tidslinjeFeriepengerInnenforKvote) {
        return String.join(",", tidslinjeFeriepengerInnenforKvote.stream().map(segment -> segment.getFom() + "-" + segment.getTom()).toList());
    }

    private LocalDateTimeline<Boolean> utledPerioderInnenforKvote(LocalDateTimeline<Boolean> tidslinjeHvorYtelseHarEllerKanGiFeriepenger, BeregningsresultatFeriepengerRegelModell regelModell) {
        Collection<LocalDateTimeline<Boolean>> aktuelleAndelerPrKvote = TidslinjeUtil.splittOgGruperPåÅrstall(tidslinjeHvorYtelseHarEllerKanGiFeriepenger).values();
        return new LocalDateTimeline<>(aktuelleAndelerPrKvote.stream()
            .map(tidslinje -> TidslinjeUtil.begrensTilAntallDager(tidslinje, regelModell.getAntallDagerFeriepenger(), regelModell.harFeriepengeopptjeningForHelg()))
            .flatMap(tidslinje -> tidslinje.toSegments().stream())
            .toList());
    }


}
