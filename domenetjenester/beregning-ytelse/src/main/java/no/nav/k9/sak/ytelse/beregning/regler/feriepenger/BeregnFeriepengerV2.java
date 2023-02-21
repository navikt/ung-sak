package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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

    public static final String ID = "FP_BR 8.6.1";
    public static final String BESKRIVELSE = "Beregn feriepenger for periode som går over flere kalenderår.";

    private static final BigDecimal FERIEPENGER_SATS = BigDecimal.valueOf(0.102);

    BeregnFeriepengerV2() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        Map<String, Object> regelsporing = new LinkedHashMap<>();

        LocalDateTimeline<Boolean> tidslinjeFeriepengerInnenforKvote;
        LocalDateTimeline<Boolean> tidslinjeFeriepengerInnenforKvoteRefusjon;

        if (regelModell.harUbegrensetFeriepengedagerVedRefusjon()) {
            //gjelder omsorsgspenger
            LocalDateTimeline<Boolean> tidslinjeYtelseKanGiFeriepenger = FeriepengerForAndelUtil.utledTidslinjerHarAndelSomKanGiFeriepengerTilBruker(regelModell.getBeregningsresultatPerioder());
            tidslinjeFeriepengerInnenforKvote = utledPerioderInnenforKvote(tidslinjeYtelseKanGiFeriepenger, regelModell);
            tidslinjeFeriepengerInnenforKvoteRefusjon = FeriepengerForAndelUtil.utledTidslinjerHarAndelSomKanGiFeriepengerTilRefusjon(regelModell.getBeregningsresultatPerioder());
            regelsporing.put("perioder-feriepenger-til-bruker", prettyPrint(tidslinjeFeriepengerInnenforKvote));
            regelsporing.put("perioder-feriepenger-ved-refusjon", prettyPrint(tidslinjeFeriepengerInnenforKvoteRefusjon));
        } else {
            LocalDateTimeline<Boolean> tidslinjeDenneSakenHarAndelSomKanGiFeriepenger = FeriepengerForAndelUtil.utledTidslinjerHarAndelSomKanGiFeriepenger(regelModell.getBeregningsresultatPerioder());
            LocalDateTimeline<Boolean> tidslinjeYtelseKanGiFeriepenger = tidslinjeDenneSakenHarAndelSomKanGiFeriepenger.union(regelModell.getAndelerSomKanGiFeriepengerForRelevaneSaker(), StandardCombinators::alwaysTrueForMatch).compress();
            tidslinjeFeriepengerInnenforKvote = utledPerioderInnenforKvote(tidslinjeYtelseKanGiFeriepenger, regelModell);
            tidslinjeFeriepengerInnenforKvoteRefusjon = tidslinjeFeriepengerInnenforKvote;
            regelsporing.put("perioder-med-feriepenger-på-tvers-av-saker", prettyPrint(tidslinjeYtelseKanGiFeriepenger));
        }

        beregn(regelsporing, regelModell, tidslinjeFeriepengerInnenforKvote, MottakerType.BRUKER);
        beregn(regelsporing, regelModell, tidslinjeFeriepengerInnenforKvoteRefusjon, MottakerType.ARBEIDSGIVER);

        return beregnet(regelsporing);
    }

    private static void beregn(Map<String, Object> regelsporing, BeregningsresultatFeriepengerRegelModell regelModell, LocalDateTimeline<Boolean> tidslinjeHvorFeriepengerGis, MottakerType mottakerType) {
        List<BeregningsresultatPeriode> beregningsresultatPerioder = regelModell.getBeregningsresultatPerioder();
        boolean harFeriepengeopptjeningForHelg = regelModell.harFeriepengeopptjeningForHelg();
        Predicate<BeregningsresultatAndel> andelFilter = andel -> andel.girRettTilFeriepenger() && andel.getMottakerType() == mottakerType;
        Map<FeriepengeNøkkel, BigDecimal> avrundingTilgode = new HashMap<>();

        for (BeregningsresultatPeriode periode : beregningsresultatPerioder) {
            LocalDateTimeline<Boolean> overlapp = tidslinjeHvorFeriepengerGis.intersection(periode.getPeriode());
            if (!overlapp.isEmpty() && periode.getBeregningsresultatAndelList().stream().anyMatch(andelFilter)) {
                for (LocalDateInterval åretsOverlapp : IntervallUtil.splittVedÅrskifte(overlapp).getLocalDateIntervals()) {
                    long antallFeriepengerDager = harFeriepengeopptjeningForHelg
                        ? IntervallUtil.beregnKalanderdager(åretsOverlapp)
                        : IntervallUtil.beregnUkedager(åretsOverlapp);

                    String periodeNavn = "perioden " + åretsOverlapp + "for " + mottakerType.name().toLowerCase();
                    regelsporing.put("Antall feriepengedager i " + periodeNavn, antallFeriepengerDager);
                    regelsporing.put("Opptjeningsår i " + periodeNavn, åretsOverlapp.getFomDato().getYear());

                    for (BeregningsresultatAndel andel : periode.getBeregningsresultatAndelList()) {
                        if (!andelFilter.test(andel)) {
                            continue;
                        }
                        long feriepengerGrunnlag = andel.getDagsats() * antallFeriepengerDager;
                        BigDecimal feriepengerAndelPrÅr = BigDecimal.valueOf(feriepengerGrunnlag).multiply(FERIEPENGER_SATS);
                        FeriepengeNøkkel nøkkel = new FeriepengeNøkkel(andel.getMottakerType(), andel.getMottakerType() == MottakerType.BRUKER ? null : andel.getArbeidsgiverId(), åretsOverlapp.getFomDato().getYear());
                        BigDecimal tidligereAvrunding = avrundingTilgode.getOrDefault(nøkkel, BigDecimal.ZERO);
                        BigDecimal endeligFeriepengerForAndelen = feriepengerAndelPrÅr.subtract(tidligereAvrunding).setScale(0, RoundingMode.HALF_UP);
                        BigDecimal avrunding = endeligFeriepengerForAndelen.subtract(feriepengerAndelPrÅr);

                        String andelId = andel.getArbeidsforhold() != null ? andel.getArbeidsgiverId() : andel.getAktivitetStatus().name();
                        if (feriepengerAndelPrÅr.compareTo(BigDecimal.ZERO) != 0) {
                            regelsporing.put("Feriepenger." + andel.getMottakerType() + "." + andelId + " i " + periodeNavn, feriepengerAndelPrÅr);
                        }
                        if (avrunding.compareTo(BigDecimal.ZERO) != 0) {
                            regelsporing.put("Feriepenger.avrunding" + andel.getMottakerType() + "." + andelId + " i " + periodeNavn, avrunding);
                        }
                        if (endeligFeriepengerForAndelen.compareTo(BigDecimal.ZERO) != 0) {
                            BeregningsresultatFeriepengerPrÅr.builder()
                                .medOpptjeningÅr(åretsOverlapp.getFomDato().withMonth(12).withDayOfMonth(31))
                                .medÅrsbeløp(endeligFeriepengerForAndelen)
                                .build(andel);
                        }
                        avrundingTilgode.put(nøkkel, tidligereAvrunding.add(avrunding));
                    }
                }
            }
        }
    }

    record FeriepengeNøkkel(MottakerType mottakerType, String mottakerId, int år) {

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
