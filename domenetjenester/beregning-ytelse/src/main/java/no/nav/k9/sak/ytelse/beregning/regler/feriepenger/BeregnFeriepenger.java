package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.domene.typer.tid.IntervallUtil;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.MottakerType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.FeriepengekorrigeringInfotrygd;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.InfotrygdFeriepengegrunnlag;

class BeregnFeriepenger extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {

    public static final String ID = "FP_BR 8.6.1 v2";
    public static final String BESKRIVELSE = "Beregn feriepenger for periode som går over flere kalenderår.";

    private static final BigDecimal FERIEPENGER_SATS = BigDecimal.valueOf(0.102);

    private static final Logger logger = LoggerFactory.getLogger(BeregnFeriepenger.class);

    BeregnFeriepenger() {
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

        beregnInfotrygdFeriepengerKorrigering(regelsporing, regelModell, tidslinjeFeriepengerInnenforKvote);

        beregn(regelsporing, regelModell, tidslinjeFeriepengerInnenforKvote, MottakerType.BRUKER);
        beregn(regelsporing, regelModell, tidslinjeFeriepengerInnenforKvoteRefusjon, MottakerType.ARBEIDSGIVER);


        return beregnet(regelsporing);
    }

    /**
     * For søknadsperioder til og med 2023 er opplæringspenger i Infotrygd og pleiepenger sykt barn er i k9-sak. Dette samtidig med at de deler kvote.
     * For å unngå feilutbetaling av feriepenger, må disse sees under ett. Valgt løsning er er to-delt.
     * <p>
     * Del 1 er at k9-sak tar med infotrygd-saken inn i egen feriepengeberegning, og slik teller dager hvor Infotrygd forbruker feriepenger.
     * <p>
     * Del 2 er at k9-sak korrigerer så godt som mulig ved å redusere egen feriepengeutbetaling når infotrygd har utbetalt feriepenger for perioder etter at kvota er brukt opp i k9-sak.
     * <p>
     * Funksjonen under dekker del 2.
     */
    private static void beregnInfotrygdFeriepengerKorrigering(Map<String, Object> regelsporing, BeregningsresultatFeriepengerRegelModell regelModell, LocalDateTimeline<Boolean> tidslinjeFeriepengerInnenforKvote) {
        InfotrygdFeriepengegrunnlag infotrygdFeriepengegrunnlag = regelModell.getInfotrygdFeriepengegrunnlag();
        if (infotrygdFeriepengegrunnlag == null) {
            regelModell.setFeriepengekorrigeringInfotrygd(List.of());
            return;
        }

        LocalDateTimeline<Boolean> feriepengedagerInfotrygd = infotrygdFeriepengegrunnlag.dagerFeriepengerFraInfotrygd();


        List<FeriepengekorrigeringInfotrygd> resultat = new ArrayList<>();
        regelsporing.put("perioder-med-feriepenger-innvilget-i-infotrygd", prettyPrint(feriepengedagerInfotrygd));
        {
            LocalDateTimeline<BigDecimal> direkteutbetaling = infotrygdFeriepengegrunnlag.tidslinjeDirekteutbetaling();
            LocalDateTimeline<BigDecimal> infotrygdFeriepengeTidslinje = InfotrygdFeriepengegrunnlag.tilFeriepengerTidslinje(direkteutbetaling, feriepengedagerInfotrygd);
            regelsporing.put("infotrygd-feriepenger.direkteutbetaling", prettyPrintMedVerdier(infotrygdFeriepengeTidslinje));
            LocalDateTimeline<BigDecimal> trengerKorrigering = infotrygdFeriepengeTidslinje.disjoint(tidslinjeFeriepengerInnenforKvote);
            NavigableMap<Year, LocalDateTimeline<BigDecimal>> korrigeringPrÅr = TidslinjeUtil.splittOgGruperPåÅrstall(trengerKorrigering);
            for (Map.Entry<Year, LocalDateTimeline<BigDecimal>> e : korrigeringPrÅr.entrySet()) {
                BigDecimal korrigering = summerFeriepenger(e.getValue());
                resultat.add(FeriepengekorrigeringInfotrygd.forBruker(e.getKey(), korrigering));
            }
        }

        infotrygdFeriepengegrunnlag.tidslinjerRefusjon().forEach((
            (arbeidsgiver, dagsatsRefusjon) -> {
                LocalDateTimeline<BigDecimal> infotrygdFeriepengeTidslinje = InfotrygdFeriepengegrunnlag.tilFeriepengerTidslinje(dagsatsRefusjon, feriepengedagerInfotrygd);
                regelsporing.put("infotrygd-feriepenger.refusjon." + arbeidsgiver.getIdentifikator(), prettyPrintMedVerdier(infotrygdFeriepengeTidslinje));
                LocalDateTimeline<BigDecimal> trengerKorrigering = infotrygdFeriepengeTidslinje.disjoint(tidslinjeFeriepengerInnenforKvote);
                NavigableMap<Year, LocalDateTimeline<BigDecimal>> korrigeringPrÅr = TidslinjeUtil.splittOgGruperPåÅrstall(trengerKorrigering);
                for (Map.Entry<Year, LocalDateTimeline<BigDecimal>> e : korrigeringPrÅr.entrySet()) {
                    BigDecimal korrigering = summerFeriepenger(e.getValue());
                    resultat.add(FeriepengekorrigeringInfotrygd.forRefusjon(e.getKey(), arbeidsgiver, korrigering));
                }
            }));
        regelModell.setFeriepengekorrigeringInfotrygd(resultat);
    }

    private static BigDecimal summerFeriepenger(LocalDateTimeline<BigDecimal> feriepenger) {
        LocalDateTimeline<BigDecimal> utenHelg = Hjelpetidslinjer.fjernHelger(feriepenger);
        return utenHelg.stream()
            .map(segment -> segment.getValue().multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(segment.getFom(), segment.getTom()) + 1)))
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    }


    private static void beregn(Map<String, Object> regelsporing, BeregningsresultatFeriepengerRegelModell regelModell, LocalDateTimeline<Boolean> tidslinjeHvorFeriepengerGis, MottakerType mottakerType) {
        List<BeregningsresultatPeriode> beregningsresultatPerioder = regelModell.getBeregningsresultatPerioder();
        boolean harFeriepengeopptjeningForHelg = regelModell.harFeriepengeopptjeningForHelg();
        Predicate<BeregningsresultatAndel> andelFilter = andel -> andel.girRettTilFeriepenger() && andel.getMottakerType() == mottakerType;
        Map<FeriepengeNøkkel, BigDecimal> avrundingTilgode = new HashMap<>();
        Map<Year, BigDecimal> infotrygdKorrigering = map(regelModell.getFeriepengekorrigeringInfotrygd());
        BigDecimal korrigeringsbehov = infotrygdKorrigering.values().stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        for (BeregningsresultatPeriode periode : beregningsresultatPerioder) {
            LocalDateTimeline<Boolean> overlapp = tidslinjeHvorFeriepengerGis.intersection(periode.getPeriode());
            if (!overlapp.isEmpty() && periode.getBeregningsresultatAndelList().stream().anyMatch(andelFilter)) {
                for (LocalDateInterval åretsOverlapp : IntervallUtil.splittVedÅrskifte(overlapp).getLocalDateIntervals()) {
                    Year opptjeningsår = Year.of(åretsOverlapp.getFomDato().getYear());
                    long antallFeriepengerDager = harFeriepengeopptjeningForHelg
                        ? IntervallUtil.beregnKalanderdager(åretsOverlapp)
                        : IntervallUtil.beregnUkedager(åretsOverlapp);

                    String periodeNavn = "perioden " + åretsOverlapp + "for " + mottakerType.name().toLowerCase();
                    regelsporing.put("Antall feriepengedager i " + periodeNavn, antallFeriepengerDager);
                    regelsporing.put("Opptjeningsår i " + periodeNavn, opptjeningsår);

                    for (BeregningsresultatAndel andel : periode.getBeregningsresultatAndelList()) {
                        if (!andelFilter.test(andel)) {
                            continue;
                        }
                        String andelId = andel.getArbeidsforhold() != null ? andel.getArbeidsgiverId() : andel.getAktivitetStatus().name();
                        long feriepengerGrunnlag = andel.getDagsats() * antallFeriepengerDager;
                        BigDecimal feriepengerAndelPrÅr = BigDecimal.valueOf(feriepengerGrunnlag).multiply(FERIEPENGER_SATS);

                        if (feriepengerAndelPrÅr.compareTo(BigDecimal.ZERO) != 0) {
                            regelsporing.put("Feriepenger." + andel.getMottakerType() + "." + andelId + " i " + periodeNavn, feriepengerAndelPrÅr);
                            if (infotrygdKorrigering.containsKey(opptjeningsår)) {
                                BigDecimal gjenståendeKorrigering = infotrygdKorrigering.get(opptjeningsår);
                                BigDecimal korreksjon = feriepengerAndelPrÅr.compareTo(gjenståendeKorrigering) > 0 ? gjenståendeKorrigering : feriepengerAndelPrÅr;
                                regelsporing.put("Feriepenger." + andel.getMottakerType() + "." + andelId + " i " + periodeNavn + " reduseres pga korrigering mot infotrygd med", korreksjon);
                                feriepengerAndelPrÅr = feriepengerAndelPrÅr.subtract(korreksjon);

                                gjenståendeKorrigering = gjenståendeKorrigering.subtract(korreksjon);
                                if (gjenståendeKorrigering.signum() == 0) {
                                    infotrygdKorrigering.remove(opptjeningsår);
                                } else {
                                    infotrygdKorrigering.put(opptjeningsår, gjenståendeKorrigering);
                                }
                                if (feriepengerAndelPrÅr.compareTo(BigDecimal.ZERO) != 0) {
                                    regelsporing.put("Feriepenger." + andel.getMottakerType() + "." + andelId + " i " + periodeNavn + " etter korrigering mot infotrygd", feriepengerAndelPrÅr);
                                }
                            }
                        }
                        FeriepengeNøkkel nøkkel = new FeriepengeNøkkel(andel.getMottakerType(), andel.getMottakerType() == MottakerType.BRUKER ? null : andel.getArbeidsgiverId(), opptjeningsår.getValue());
                        BigDecimal tidligereAvrunding = avrundingTilgode.getOrDefault(nøkkel, BigDecimal.ZERO);
                        BigDecimal endeligFeriepengerForAndelen = feriepengerAndelPrÅr.subtract(tidligereAvrunding).setScale(0, RoundingMode.HALF_UP);
                        BigDecimal avrunding = endeligFeriepengerForAndelen.subtract(feriepengerAndelPrÅr);


                        if (avrunding.compareTo(BigDecimal.ZERO) != 0) {
                            regelsporing.put("Feriepenger.avrunding." + andel.getMottakerType() + "." + andelId + " i " + periodeNavn, avrunding);
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
        if (korrigeringsbehov.signum() != 0) {
            BigDecimal gjenståendeKorrigeringsbehov = infotrygdKorrigering.values().stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            if (gjenståendeKorrigeringsbehov.signum() == 0) {
                logger.info("Korrigerte feilutbetalte feriepenger fra infotrygd.");
            } else {
                logger.warn("Klarte ikke å korrigere 100% av feilutbetalte feriepenger fra infotrygd. Gjenstår {} % av det som skulle korrigeres.", BigDecimal.valueOf(100).multiply(gjenståendeKorrigeringsbehov).divide(korrigeringsbehov, 0, RoundingMode.HALF_UP));
            }
        }
    }

    private static Map<Year, BigDecimal> map(List<FeriepengekorrigeringInfotrygd> feriepengekorrigeringInfotrygd) {
        Map<Year, BigDecimal> summertKorrigeringsbeløp = feriepengekorrigeringInfotrygd.stream()
            .collect(Collectors.toMap(FeriepengekorrigeringInfotrygd::getOpptjeningsår, FeriepengekorrigeringInfotrygd::getKorrigeringsbeløp, BigDecimal::add));
        return summertKorrigeringsbeløp.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e->e.getValue().setScale(2, RoundingMode.HALF_UP)));
    }

    record FeriepengeNøkkel(MottakerType mottakerType, String mottakerId, int år) {

    }

    private static String prettyPrint(LocalDateTimeline<Boolean> tidslinje) {
        return String.join(",", tidslinje.stream().map(segment -> "[" + segment.getFom() + "," + segment.getTom() + "]").toList());
    }

    private static String prettyPrintMedVerdier(LocalDateTimeline<BigDecimal> tidlinje) {
        return String.join(",", tidlinje.stream().map(segment -> "[" + segment.getFom() + "," + segment.getTom() + "]:" + segment.getValue().setScale(2, RoundingMode.HALF_UP)).toList());
    }

    private LocalDateTimeline<Boolean> utledPerioderInnenforKvote(LocalDateTimeline<Boolean> tidslinjeHvorYtelseHarEllerKanGiFeriepenger, BeregningsresultatFeriepengerRegelModell regelModell) {
        Collection<LocalDateTimeline<Boolean>> aktuelleAndelerPrKvote = TidslinjeUtil.splittOgGruperPåÅrstall(tidslinjeHvorYtelseHarEllerKanGiFeriepenger).values();
        return new LocalDateTimeline<>(aktuelleAndelerPrKvote.stream()
            .map(tidslinje -> TidslinjeUtil.begrensTilAntallDager(tidslinje, regelModell.getAntallDagerFeriepenger(), regelModell.harFeriepengeopptjeningForHelg()))
            .flatMap(tidslinje -> tidslinje.toSegments().stream())
            .toList());
    }


}
