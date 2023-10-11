package no.nav.k9.sak.ytelse.beregning.regler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.ytelse.beregning.adapter.AktivitetStatusMapper;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatRegelmodellMellomregning;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;

class FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder extends LeafSpecification<BeregningsresultatRegelmodellMellomregning> {
    public static final String ID = "FP_BR 20_1";
    public static final String BESKRIVELSE = "FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder";
    private static final Period MAKS_FREMTID = Period.parse("P1Y");
    private static final Period SPLITT_PERIODE = Period.parse("P1Y");
    private static final String BRUKER_ANDEL = ".brukerAndel";
    private static final String ARBEIDSGIVERS_ANDEL = ".arbeidsgiverAndel";
    private static final String DAGSATS_BRUKER = ".dagsatsBruker";
    private static final String DAGSATS_ARBEIDSGIVER = ".dagsatsArbeidsgiver";
    private static final String ARBEIDSGIVER_ID = ".arbeidsgiverId";

    FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder() {
        super(ID, BESKRIVELSE);
    }

    private static long årsbeløpTilDagsats(BigDecimal årsbeløp) {
        final BigDecimal toHundreSeksti = BigDecimal.valueOf(260);
        return årsbeløp.divide(toHundreSeksti, 0, RoundingMode.HALF_UP).longValue();
    }

    @Override
    public Evaluation evaluate(BeregningsresultatRegelmodellMellomregning mellomregning) {
        // Regelsporing
        Map<String, Object> resultater = new LinkedHashMap<>();

        BeregningsresultatRegelmodell regelmodell = mellomregning.getInput();
        List<Beregningsgrunnlag> grunnlagene = regelmodell.getBeregningsgrunnlag();
        UttakResultat uttakResultat = regelmodell.getUttakResultat();

        List<BeregningsresultatPeriode> periodeListe = mapPerioder(grunnlagene, uttakResultat, resultater, mellomregning.getInput().getSkalVurdereGjelderFor());
        periodeListe.forEach(p -> mellomregning.getOutput().addBeregningsresultatPeriode(p));
        return beregnet(resultater);
    }

    private List<BeregningsresultatPeriode> mapPerioder(List<Beregningsgrunnlag> grunnlag, UttakResultat uttakResultat, Map<String, Object> resultater, boolean skalVurdereGjelderFor) {
        LocalDateTimeline<BeregningsgrunnlagPeriode> grunnlagTimeline = mapGrunnlagTimeline(grunnlag);
        LocalDateTimeline<List<UttakResultatPeriode>> uttakTimeline = uttakResultat.getUttakPeriodeTimelineMedOverlapp();
        LocalDateTimeline<BeregningsresultatPeriode> resultatTimeline = intersectTimelines(grunnlagTimeline, uttakTimeline, resultater, skalVurdereGjelderFor)
            .compress();
        return resultatTimeline.toSegments().stream().map(LocalDateSegment::getValue).collect(Collectors.toList());
    }

    private LocalDateTimeline<BeregningsresultatPeriode> intersectTimelines(LocalDateTimeline<BeregningsgrunnlagPeriode> grunnlagTimeline, LocalDateTimeline<List<UttakResultatPeriode>> uttakTimeline,
                                                                            Map<String, Object> resultater, boolean skalVurdereGjelderFor) {

        if (grunnlagTimeline.isEmpty() || uttakTimeline.isEmpty()) {
            return LocalDateTimeline.empty();
        }

        var startFørsteÅr = grunnlagTimeline.getMinLocalDate().withDayOfYear(1);
        var grunnlagMaksDato = grunnlagTimeline.getMaxLocalDate();
        var uttakMaksDato = sisteDagMedUtbetaling(uttakTimeline);

        var nyttårsaftenEtÅrFremITid = LocalDate.now().withMonth(12).withDayOfMonth(31).plus(MAKS_FREMTID);
        if (uttakMaksDato.isAfter(nyttårsaftenEtÅrFremITid)) {
            throw new IllegalArgumentException("Uttaksplan kan ikke være åpen eller for langt frem i tid. Uttak maksdato:'" + uttakMaksDato + "', utbetaling maksdato: '" + nyttårsaftenEtÅrFremITid + "'");
        }

        // stopper periodisering her for å unngå 'evigvarende' ekspansjon -
        // tar første av potensielle maks datoer som berører intersection av de to tidslinjene.
        var minsteMaksDato = Stream.of(grunnlagMaksDato, uttakTimeline.getMaxLocalDate()).sorted().findFirst().orElseThrow();

        var grunnlagTimelinePeriodisertÅr = grunnlagTimeline.splitAtRegular(startFørsteÅr, minsteMaksDato, SPLITT_PERIODE);

        final int[] i = {0}; // Periode-teller til regelsporing
        return grunnlagTimelinePeriodisertÅr.intersection(uttakTimeline, (dateInterval, grunnlagSegment, uttakSegment) -> {
            BeregningsgrunnlagPeriode grunnlag = grunnlagSegment.getValue();
            List<UttakResultatPeriode> uttakResultatPeriode = uttakSegment.getValue();

            BeregningsresultatPeriode resultatPeriode = new BeregningsresultatPeriode(
                dateInterval,
                grunnlag.getInntektGraderingsprosent(),
                grunnlag.getTotalUtbetalingsgradFraUttak(),
                grunnlag.getTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt(),
                grunnlag.getReduksjonsfaktorInaktivTypeA(),
                grunnlag.getGraderingsfaktorTid(),
                grunnlag.getGraderingsfaktorInntekt());

            // Regelsporing
            String periodeNavn = "BeregningsresultatPeriode[" + i[0] + "]";
            resultater.put(periodeNavn + ".fom", dateInterval.getFomDato());
            resultater.put(periodeNavn + ".tom", dateInterval.getTomDato());


            grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).forEach(gbps -> {
                // for hver arbeidstaker andel: map fra grunnlag til 1-2 resultatAndel
                List<BeregningsgrunnlagPrArbeidsforhold> arbeidsforholdList = gbps.getArbeidsforhold();
                arbeidsforholdList.forEach(a -> uttakResultatPeriode.forEach(up -> opprettBeregningsresultatAndelerATFL(grunnlag, a, resultatPeriode, resultater, periodeNavn, up, skalVurdereGjelderFor)));
            });
            grunnlag.getBeregningsgrunnlagPrStatus().stream()
                .filter(bgps -> !AktivitetStatus.ATFL.equals(bgps.getAktivitetStatus()))
                .forEach(bgps -> uttakResultatPeriode.forEach(up -> opprettBeregningsresultatAndelerGenerell(grunnlag, bgps, resultatPeriode, resultater, periodeNavn, up)));

            i[0]++;
            return new LocalDateSegment<>(dateInterval, resultatPeriode);
        });
    }


    private LocalDate sisteDagMedUtbetaling(LocalDateTimeline<List<UttakResultatPeriode>> uttakTimeline) {
        var maksUtbetalingsDato = uttakTimeline.stream()
            .filter(it -> it.getValue()
                .stream()
                .noneMatch(at -> at.getUttakAktiviteter().stream().allMatch(ad -> BigDecimal.ZERO.compareTo(ad.getUtbetalingsgrad()) == 0)))
            .map(LocalDateSegment::getTom)
            .max(LocalDate::compareTo)
            .orElse(uttakTimeline.getMinLocalDate()); // Setter til MIN date, da det ikke er noen utbetaling i hele tidslinjen. Det bør ikke stoppe her
        return maksUtbetalingsDato;
    }

    private void opprettBeregningsresultatAndelerGenerell(BeregningsgrunnlagPeriode grunnlag, BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus, BeregningsresultatPeriode resultatPeriode,
                                                          Map<String, Object> resultater, String periodeNavn, UttakResultatPeriode uttakResultatPeriode) {
        if (uttakResultatPeriode.getErOppholdsPeriode()) {
            return;
        }
        Optional<UttakAktivitet> uttakAktivitetOpt = matchUttakAktivitetMedBeregningsgrunnlagPrStatus(beregningsgrunnlagPrStatus, uttakResultatPeriode.getUttakAktiviteter());
        if (uttakAktivitetOpt.isEmpty()) {
            return;
        }
        UttakAktivitet uttakAktivitet = uttakAktivitetOpt.get();

        // Fra dagsats gradert ifht utbetalingsgrad
        long dagsatsBruker = årsbeløpTilDagsats(beregningsgrunnlagPrStatus.getRedusertBrukersAndelPrÅr());
        BigDecimal utbetalingsgradOppdrag = utbetalingsgradOppdrag(beregningsgrunnlagPrStatus.getRedusertBrukersAndelPrÅr(), grunnlag);
        resultatPeriode.addBeregningsresultatAndel(
            BeregningsresultatAndel.builder()
                .medBrukerErMottaker(true)
                .medDagsats(dagsatsBruker)
                .medDagsatsFraBg(årsbeløpTilDagsats(beregningsgrunnlagPrStatus.getRedusertBrukersAndelPrÅr()))
                .medAktivitetStatus(beregningsgrunnlagPrStatus.getAktivitetStatus())
                .medInntektskategori(beregningsgrunnlagPrStatus.getInntektskategori())
                .medUtbetalingssgrad(uttakAktivitet.getUtbetalingsgrad())
                .medUtbetalingssgradOppdrag(utbetalingsgradOppdrag)
                .medStillingsprosent(uttakAktivitet.getStillingsgrad())
                .build(resultatPeriode));

        // Regelsporing
        String beskrivelse = periodeNavn + BRUKER_ANDEL + "['" + beregningsgrunnlagPrStatus.getAktivitetStatus().name() + "']" + DAGSATS_BRUKER;
        resultater.put(beskrivelse, dagsatsBruker);
    }

    private static BigDecimal utbetalingsgradOppdrag(BigDecimal redusertBrukersAndelPrÅr, BeregningsgrunnlagPeriode grunnlag) {
        boolean erMidlertidigInaktivTypeA = grunnlag.getReduksjonsfaktorInaktivTypeA() != null;
        BigDecimal maksimalUtbetaling = erMidlertidigInaktivTypeA
            ? grunnlag.getBruttoBeregningsgrunnlag().multiply(grunnlag.getReduksjonsfaktorInaktivTypeA()).setScale(0, RoundingMode.HALF_UP)
            : grunnlag.getBruttoBeregningsgrunnlag();
        return prosentAvMaksimal(redusertBrukersAndelPrÅr, maksimalUtbetaling);
    }

    private static BigDecimal prosentAvMaksimal(BigDecimal input, BigDecimal maks) {
        if (maks.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(100).multiply(input).divide(maks, 2, RoundingMode.HALF_UP);
    }

    private Optional<UttakAktivitet> matchUttakAktivitetMedBeregningsgrunnlagPrStatus(BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus, List<UttakAktivitet> uttakAktiviteter) {
        return uttakAktiviteter.stream()
            .filter(aktivitet -> {
                var aktivitetStatus = beregningsgrunnlagPrStatus.getAktivitetStatus();
                if ((aktivitet.getType().equals(UttakArbeidType.ANNET))) {
                    return !aktivitetStatus.erGraderbar();
                } else {
                    var utAktivitet = AktivitetStatusMapper.fraVLTilRegel(aktivitet.getType().getAktivitetStatus());
                    return Objects.equals(utAktivitet, aktivitetStatus);
                }
            })
            .findFirst();
    }

    private void opprettBeregningsresultatAndelerATFL(BeregningsgrunnlagPeriode grunnlag, BeregningsgrunnlagPrArbeidsforhold arbeidsforhold, BeregningsresultatPeriode resultatPeriode,
                                                      Map<String, Object> resultater, String periodeNavn, UttakResultatPeriode uttakResultatPeriode, boolean skalVurdereGjelderFor) {
        if (uttakResultatPeriode.getErOppholdsPeriode()) {
            return;
        }
        Optional<UttakAktivitet> uttakAktivitetOpt = matchUttakAktivitetMedArbeidsforhold(uttakResultatPeriode.getUttakAktiviteter(), arbeidsforhold, skalVurdereGjelderFor);
        if (uttakAktivitetOpt.isEmpty()) {
            return;
        }
        UttakAktivitet uttakAktivitet = uttakAktivitetOpt.get();
        String arbeidsgiverId = arbeidsforhold.getArbeidsgiverId();

        // Fra dagsats gradert ifht utbetalingsgrad
        Long dagsatsBruker = årsbeløpTilDagsats(arbeidsforhold.getRedusertBrukersAndelPrÅr());
        Long dagsatsArbeidsgiver = årsbeløpTilDagsats(arbeidsforhold.getRedusertRefusjonPrÅr());

        BigDecimal utbetalingsgradOppdragBruker = utbetalingsgradOppdrag(arbeidsforhold.getRedusertBrukersAndelPrÅr(), grunnlag);
        BigDecimal utbetalingsgradOppdragRefusjon = utbetalingsgradOppdrag(arbeidsforhold.getRedusertRefusjonPrÅr(), grunnlag);


        resultatPeriode.addBeregningsresultatAndel(
            BeregningsresultatAndel.builder()
                .medArbeidsforhold(arbeidsforhold.getArbeidsforhold())
                .medBrukerErMottaker(true)
                .medStillingsprosent(uttakAktivitet.getStillingsgrad())
                .medUtbetalingssgrad(uttakAktivitet.getUtbetalingsgrad())
                .medUtbetalingssgradOppdrag(utbetalingsgradOppdragBruker)
                .medDagsats(dagsatsBruker)
                .medDagsatsFraBg(arbeidsforhold.getDagsatsBruker())
                .medAktivitetStatus(AktivitetStatus.ATFL)
                .medInntektskategori(arbeidsforhold.getInntektskategori())
                .build(resultatPeriode));

        // Regelsporing
        resultater.put(periodeNavn + BRUKER_ANDEL + "['" + arbeidsgiverId + "']" + ARBEIDSGIVER_ID, arbeidsgiverId);
        resultater.put(periodeNavn + BRUKER_ANDEL + "['" + arbeidsgiverId + "']" + DAGSATS_BRUKER, dagsatsBruker);
        resultater.put(periodeNavn + BRUKER_ANDEL + "['" + arbeidsgiverId + "']" + ".dagsatsFraBeregningsgrunnlagBruker", arbeidsforhold.getDagsatsBruker());

        if (arbeidsforhold.getDagsatsArbeidsgiver() != null && arbeidsforhold.getDagsatsArbeidsgiver() > 0) {
            resultatPeriode.addBeregningsresultatAndel(
                BeregningsresultatAndel.builder()
                    .medArbeidsforhold(arbeidsforhold.getArbeidsforhold())
                    .medBrukerErMottaker(false)
                    .medStillingsprosent(uttakAktivitet.getStillingsgrad())
                    .medUtbetalingssgrad(uttakAktivitet.getUtbetalingsgrad())
                    .medUtbetalingssgradOppdrag(utbetalingsgradOppdragRefusjon)
                    .medDagsats(dagsatsArbeidsgiver)
                    .medDagsatsFraBg(arbeidsforhold.getDagsatsArbeidsgiver())
                    .medInntektskategori(arbeidsforhold.getInntektskategori())
                    .medAktivitetStatus(AktivitetStatus.ATFL)
                    .build(resultatPeriode));

            // Regelsporing
            resultater.put(periodeNavn + ARBEIDSGIVERS_ANDEL + "['" + arbeidsgiverId + "']" + ARBEIDSGIVER_ID, arbeidsgiverId);
            resultater.put(periodeNavn + ARBEIDSGIVERS_ANDEL + "['" + arbeidsgiverId + "']" + DAGSATS_ARBEIDSGIVER, dagsatsArbeidsgiver);
            resultater.put(periodeNavn + ARBEIDSGIVERS_ANDEL + "['" + arbeidsgiverId + "']" + ".dagsatsFraBeregningsgrunnlagArbeidsgiver", arbeidsforhold.getDagsatsArbeidsgiver());
        }
    }

    private Optional<UttakAktivitet> matchUttakAktivitetMedArbeidsforhold(List<UttakAktivitet> uttakAktiviteter, BeregningsgrunnlagPrArbeidsforhold arbeidsforhold, boolean skalVurdereGjelderFor) {
        var relevanteAktiviteterFraUttak = uttakAktiviteter
            .stream()
            .filter(uttakAktivitet -> matcher(arbeidsforhold, uttakAktivitet, skalVurdereGjelderFor))
            .collect(Collectors.toList());

        if (relevanteAktiviteterFraUttak.size() > 1) {
            throw new IllegalStateException("Fant flere relevante uttakaktiviteter enn forventet: " + relevanteAktiviteterFraUttak);
        }

        return relevanteAktiviteterFraUttak
            .stream()
            .findFirst();
    }

    private boolean matcher(BeregningsgrunnlagPrArbeidsforhold arbeidsforhold, UttakAktivitet uttakAktivitet, boolean skalVurdereGjelderFor) {
        var uttakArbeidsforhold = uttakAktivitet.getArbeidsforhold();
        var beregningArbeidsforhold = arbeidsforhold.getArbeidsforhold();
        if (skalVurdereGjelderFor && uttakArbeidsforhold != null && beregningArbeidsforhold != null) {
            return beregningArbeidsforhold.gjelderFor(uttakArbeidsforhold);
        }
        return Objects.equals(uttakArbeidsforhold, beregningArbeidsforhold);
    }

    private LocalDateTimeline<BeregningsgrunnlagPeriode> mapGrunnlagTimeline(List<Beregningsgrunnlag> grunnlag) {
        var grunnlagTimeline = new LocalDateTimeline<BeregningsgrunnlagPeriode>(List.of());
        for (Beregningsgrunnlag beregningsgrunnlag : grunnlag) {
            List<LocalDateSegment<BeregningsgrunnlagPeriode>> grunnlagPerioder = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
                .map(p -> new LocalDateSegment<>(p.getBeregningsgrunnlagPeriode().getFom(), p.getBeregningsgrunnlagPeriode().getTom(), p))
                .collect(Collectors.toList());
            grunnlagTimeline = grunnlagTimeline.combine(new LocalDateTimeline<>(grunnlagPerioder), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return grunnlagTimeline;
    }
}
