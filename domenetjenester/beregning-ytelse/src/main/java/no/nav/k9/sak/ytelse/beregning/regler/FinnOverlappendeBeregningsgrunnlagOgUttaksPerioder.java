package no.nav.k9.sak.ytelse.beregning.regler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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

        List<BeregningsresultatPeriode> periodeListe = mapPerioder(grunnlagene, uttakResultat, resultater);
        periodeListe.forEach(p -> mellomregning.getOutput().addBeregningsresultatPeriode(p));
        return beregnet(resultater);
    }

    private List<BeregningsresultatPeriode> mapPerioder(List<Beregningsgrunnlag> grunnlag, UttakResultat uttakResultat, Map<String, Object> resultater) {
        LocalDateTimeline<BeregningsgrunnlagPeriode> grunnlagTimeline = mapGrunnlagTimeline(grunnlag);
        LocalDateTimeline<List<UttakResultatPeriode>> uttakTimeline = uttakResultat.getUttakPeriodeTimelineMedOverlapp();
        LocalDateTimeline<BeregningsresultatPeriode> resultatTimeline = intersectTimelines(grunnlagTimeline, uttakTimeline, resultater)
            .compress();
        return resultatTimeline.toSegments().stream().map(LocalDateSegment::getValue).collect(Collectors.toList());
    }

    private LocalDateTimeline<BeregningsresultatPeriode> intersectTimelines(LocalDateTimeline<BeregningsgrunnlagPeriode> grunnlagTimeline, LocalDateTimeline<List<UttakResultatPeriode>> uttakTimeline,
                                                                            Map<String, Object> resultater) {
        final int[] i = {0}; // Periode-teller til regelsporing
        return grunnlagTimeline.intersection(uttakTimeline, (dateInterval, grunnlagSegment, uttakSegment) -> {
            BeregningsresultatPeriode resultatPeriode = BeregningsresultatPeriode.builder()
                .medPeriode(dateInterval).build();

            // Regelsporing
            String periodeNavn = "BeregningsresultatPeriode[" + i[0] + "]";
            resultater.put(periodeNavn + ".fom", dateInterval.getFomDato());
            resultater.put(periodeNavn + ".tom", dateInterval.getTomDato());

            BeregningsgrunnlagPeriode grunnlag = grunnlagSegment.getValue();
            List<UttakResultatPeriode> uttakResultatPeriode = uttakSegment.getValue();

            grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).forEach(gbps -> {
                // for hver arbeidstaker andel: map fra grunnlag til 1-2 resultatAndel
                List<BeregningsgrunnlagPrArbeidsforhold> arbeidsforholdList = gbps.getArbeidsforhold();
                arbeidsforholdList.forEach(a -> uttakResultatPeriode.forEach(up -> opprettBeregningsresultatAndelerATFL(a, resultatPeriode, resultater, periodeNavn, up)));
            });
            grunnlag.getBeregningsgrunnlagPrStatus().stream()
                .filter(bgps -> !AktivitetStatus.ATFL.equals(bgps.getAktivitetStatus()))
                .forEach(bgps -> uttakResultatPeriode.forEach(up -> opprettBeregningsresultatAndelerGenerell(bgps, resultatPeriode, resultater, periodeNavn, up)));

            i[0]++;
            return new LocalDateSegment<>(dateInterval, resultatPeriode);
        });
    }

    private void opprettBeregningsresultatAndelerGenerell(BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus, BeregningsresultatPeriode resultatPeriode,
                                                          Map<String, Object> resultater, String periodeNavn, UttakResultatPeriode uttakResultatPeriode) {
        if (uttakResultatPeriode.getErOppholdsPeriode()) {
            return;
        }
        Optional<UttakAktivitet> uttakAktivitetOpt = matchUttakAktivitetMedBeregningsgrunnlagPrStatus(beregningsgrunnlagPrStatus, uttakResultatPeriode.getUttakAktiviteter());
        if (!uttakAktivitetOpt.isPresent()) {
            return;
        }
        UttakAktivitet uttakAktivitet = uttakAktivitetOpt.get();

        // Fra dagsats gradert ifht utbetalingsgrad
        Long dagsatsBruker = årsbeløpTilDagsats(beregningsgrunnlagPrStatus.getRedusertBrukersAndelPrÅr());

        resultatPeriode.addBeregningsresultatAndel(
            BeregningsresultatAndel.builder()
                .medBrukerErMottaker(true)
                .medDagsats(dagsatsBruker)
                .medDagsatsFraBg(årsbeløpTilDagsats(beregningsgrunnlagPrStatus.getRedusertBrukersAndelPrÅr()))
                .medAktivitetStatus(beregningsgrunnlagPrStatus.getAktivitetStatus())
                .medInntektskategori(beregningsgrunnlagPrStatus.getInntektskategori())
                .medUtbetalingssgrad(uttakAktivitet.getUtbetalingsgrad())
                .medStillingsprosent(uttakAktivitet.getStillingsgrad())
                .build(resultatPeriode));

        // Regelsporing
        String beskrivelse = periodeNavn + BRUKER_ANDEL + "['" + beregningsgrunnlagPrStatus.getAktivitetStatus().name() + "']" + DAGSATS_BRUKER;
        resultater.put(beskrivelse, dagsatsBruker);

    }

    private Optional<UttakAktivitet> matchUttakAktivitetMedBeregningsgrunnlagPrStatus(BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus, List<UttakAktivitet> uttakAktiviteter) {

        var match = uttakAktiviteter.stream()
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
        return match;
    }

    private void opprettBeregningsresultatAndelerATFL(BeregningsgrunnlagPrArbeidsforhold arbeidsforhold, BeregningsresultatPeriode resultatPeriode,
                                                      Map<String, Object> resultater, String periodeNavn, UttakResultatPeriode uttakResultatPeriode) {
        if (uttakResultatPeriode.getErOppholdsPeriode()) {
            return;
        }
        Optional<UttakAktivitet> uttakAktivitetOpt = matchUttakAktivitetMedArbeidsforhold(uttakResultatPeriode.getUttakAktiviteter(), arbeidsforhold);
        if (!uttakAktivitetOpt.isPresent()) {
            return;
        }
        UttakAktivitet uttakAktivitet = uttakAktivitetOpt.get();
        String arbeidsgiverId = arbeidsforhold.getArbeidsgiverId();

        // Fra dagsats gradert ifht utbetalingsgrad
        Long dagsatsBruker = årsbeløpTilDagsats(arbeidsforhold.getRedusertBrukersAndelPrÅr());
        Long dagsatsArbeidsgiver = årsbeløpTilDagsats(arbeidsforhold.getRedusertRefusjonPrÅr());

        resultatPeriode.addBeregningsresultatAndel(
            BeregningsresultatAndel.builder()
                .medArbeidsforhold(arbeidsforhold.getArbeidsforhold())
                .medBrukerErMottaker(true)
                .medStillingsprosent(uttakAktivitet.getStillingsgrad())
                .medUtbetalingssgrad(uttakAktivitet.getUtbetalingsgrad())
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

    private Optional<UttakAktivitet> matchUttakAktivitetMedArbeidsforhold(List<UttakAktivitet> uttakAktiviteter, BeregningsgrunnlagPrArbeidsforhold arbeidsforhold) {
        return uttakAktiviteter
            .stream()
            .filter(uttakAktivitet -> Objects.equals(uttakAktivitet.getArbeidsforhold(), arbeidsforhold.getArbeidsforhold()))
            .findFirst();
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
