package no.nav.k9.sak.ytelse.beregning.regler.feriepenger;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

public class RegelBeregnFeriepengerTest {

    private Arbeidsforhold arbeidsforhold1 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("123456789");
    private Arbeidsforhold arbeidsforhold2 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("234567890");


    //Eksempler tatt fra https://confluence.adeo.no/display/MODNAV/27c+Beregn+feriepenger+PK-51965+OMR-49

    //Eksempel 1 Mor
    @Test
    public void skalBeregneFeriepengerForSøker() {
        BeregningsresultatPeriode periode1 = new BeregningsresultatPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        BeregningsresultatPeriode periode2 = new BeregningsresultatPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 3, 16));
        byggAndelerForPeriode(periode1, 350, 600, arbeidsforhold1);
        byggAndelerForPeriode(periode1, 100, 500, arbeidsforhold2);
        byggAndelerForPeriode(periode2, 150, 400, arbeidsforhold1);

        BeregningsresultatFeriepengerRegelModell regelModell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2))
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medAntallDagerFeriepenger(60)
            .medAndelerSomKanGiFeriepengerForRelevaneSaker(LocalDateTimeline.empty())
            .build();

        RegelBeregnFeriepenger regel = new RegelBeregnFeriepenger();
        Evaluation evaluation = regel.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();

        regelModell.getBeregningsresultatPerioder().stream().flatMap(p -> p.getBeregningsresultatAndelList().stream())
            .forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        BeregningsresultatAndel andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel andelArbeidsgiver1 = periode1.getBeregningsresultatAndelList().get(1);
        BeregningsresultatAndel andelBruker2 = periode1.getBeregningsresultatAndelList().get(2);
        BeregningsresultatAndel andelArbeidsgiver2 = periode1.getBeregningsresultatAndelList().get(3);
        BeregningsresultatAndel andelBruker3 = periode2.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel andelArbeidsgiver3 = periode2.getBeregningsresultatAndelList().get(1);

        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1607));
        assertThat(andelBruker2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(459));
        assertThat(andelArbeidsgiver1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2754));
        assertThat(andelArbeidsgiver2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2295));
        assertThat(andelBruker3.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(76));
        assertThat(andelArbeidsgiver3.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(204));
    }

    //Eksempel 1X Mor med avslag i første periode
    @Test
    public void skalBeregneFeriepengerForSøkerMedAvslagIFørstePeriode() {
        BeregningsresultatPeriode periode0 = new BeregningsresultatPeriode(LocalDate.of(2018, 1, 5), LocalDate.of(2018, 1, 5));
        BeregningsresultatPeriode periode1 = new BeregningsresultatPeriode(LocalDate.of(2018, 1, 6), LocalDate.of(2018, 3, 9));
        BeregningsresultatPeriode periode2 = new BeregningsresultatPeriode(LocalDate.of(2018, 3, 10), LocalDate.of(2018, 3, 23));
        byggAndelerForPeriode(periode0, 0, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode1, 350, 600, arbeidsforhold1);
        byggAndelerForPeriode(periode1, 100, 500, arbeidsforhold2);
        byggAndelerForPeriode(periode2, 150, 400, arbeidsforhold1);

        BeregningsresultatFeriepengerRegelModell regelModell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(periode0, periode1, periode2))
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medAntallDagerFeriepenger(60)
            .medAndelerSomKanGiFeriepengerForRelevaneSaker(LocalDateTimeline.empty())
            .build();

        RegelBeregnFeriepenger regel = new RegelBeregnFeriepenger();
        Evaluation evaluation = regel.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();

        regelModell.getBeregningsresultatPerioder().stream().flatMap(p -> p.getBeregningsresultatAndelList().stream())
            .forEach(andel -> {
                if (andel.getDagsats() > 0) {
                    assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1);
                } else {
                    assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty();
                }
            });
        BeregningsresultatAndel andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel andelArbeidsgiver1 = periode1.getBeregningsresultatAndelList().get(1);
        BeregningsresultatAndel andelBruker2 = periode1.getBeregningsresultatAndelList().get(2);
        BeregningsresultatAndel andelArbeidsgiver2 = periode1.getBeregningsresultatAndelList().get(3);
        BeregningsresultatAndel andelBruker3 = periode2.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel andelArbeidsgiver3 = periode2.getBeregningsresultatAndelList().get(1);

        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1607));
        assertThat(andelBruker2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(459));
        assertThat(andelArbeidsgiver1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2754));
        assertThat(andelArbeidsgiver2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2295));
        assertThat(andelBruker3.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(153));
        assertThat(andelArbeidsgiver3.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(408));
    }


    //Eksempel 2 Mor
    @Test
    public void skalBeregneFeriepengerForSøkerEksempel2() {
        BeregningsresultatPeriode periode1 = new BeregningsresultatPeriode(LocalDate.of(2018, 1, 17), LocalDate.of(2018, 3, 20));
        BeregningsresultatPeriode periode2 = new BeregningsresultatPeriode(LocalDate.of(2018, 3, 21), LocalDate.of(2018, 3, 28));
        BeregningsresultatPeriode periode3 = new BeregningsresultatPeriode(LocalDate.of(2018, 3, 29), LocalDate.of(2018, 4, 8));
        byggAndelerForPeriode(periode1, 350, 600, arbeidsforhold1);
        byggAndelerForPeriode(periode1, 100, 500, arbeidsforhold2);
        byggAndelerForPeriode(periode2, 0, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode3, 350, 600, arbeidsforhold1);

        BeregningsresultatPeriode periode1annenPart = new BeregningsresultatPeriode(LocalDate.of(2018, 3, 21), LocalDate.of(2018, 4, 15));
        byggAndelerForPeriode(periode1annenPart, 500, 0, arbeidsforhold1);

        BeregningsresultatFeriepengerRegelModell regelModell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2, periode3))
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medAntallDagerFeriepenger(60)
            .medAndelerSomKanGiFeriepengerForRelevaneSaker(LocalDateTimeline.empty())
            .build();

        RegelBeregnFeriepenger regel = new RegelBeregnFeriepenger();
        Evaluation evaluation = regel.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();

        assertThat(regelModell.getBeregningsresultatPerioder().stream().flatMap(p -> p.getBeregningsresultatAndelList().stream())
            .flatMap(a -> a.getBeregningsresultatFeriepengerPrÅrListe().stream()).collect(Collectors.toList())).hasSize(6);
        periode1.getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        periode2.getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty());
        periode3.getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));

        BeregningsresultatAndel andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel andelArbeidsgiver1 = periode1.getBeregningsresultatAndelList().get(1);
        BeregningsresultatAndel andelBruker2 = periode1.getBeregningsresultatAndelList().get(2);
        BeregningsresultatAndel andelArbeidsgiver2 = periode1.getBeregningsresultatAndelList().get(3);
        BeregningsresultatAndel andelBruker4 = periode3.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel andelArbeidsgiver4 = periode3.getBeregningsresultatAndelList().get(1);

        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1607));
        assertThat(andelBruker2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(459));
        assertThat(andelArbeidsgiver1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2754));
        assertThat(andelArbeidsgiver2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2295));
        assertThat(andelBruker4.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(249));
        assertThat(andelArbeidsgiver4.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(428));
    }

    @Test
    public void skalBeregneFeriepengerOverFlereÅr() {
        BeregningsresultatPeriode periode1 = new BeregningsresultatPeriode(LocalDate.of(2018, 11, 1), LocalDate.of(2019, 1, 5)); // 47 ukedager (hvorav 4 i 2019)
        BeregningsresultatPeriode periode2 = new BeregningsresultatPeriode(LocalDate.of(2019, 1, 6), LocalDate.of(2019, 2, 5)); // 22 ukedager
        BeregningsresultatPeriode periode3 = new BeregningsresultatPeriode(LocalDate.of(2019, 2, 6), LocalDate.of(2019, 5, 16)); // 50 ukedager
        byggAndelerForPeriode(periode1, 1000, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode2, 0, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode3, 500, 500, arbeidsforhold1);

        BeregningsresultatFeriepengerRegelModell regelModell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2, periode3))
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medAntallDagerFeriepenger(60)
            .medAndelerSomKanGiFeriepengerForRelevaneSaker(LocalDateTimeline.empty())
            .build();

        RegelBeregnFeriepenger regel = new RegelBeregnFeriepenger();
        Evaluation evaluation = regel.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();

        regelModell.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(2));
        regelModell.getBeregningsresultatPerioder().get(1).getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty());
        regelModell.getBeregningsresultatPerioder().get(2).getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        BeregningsresultatAndel andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel andelBruker2 = periode3.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel andelArbeidsgiver = periode3.getBeregningsresultatAndelList().get(1);


        BigDecimal periode1feriepenger2018 = andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp();
        BigDecimal periode1feriepenger2019 = andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(1).getÅrsbeløp();
        BigDecimal periode3feriepengerBruker2019 = andelBruker2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp();
        BigDecimal periode3feriepengerRefusjon2019 = andelArbeidsgiver.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp();

        assertThat(periode1feriepenger2018).isEqualByComparingTo(BigDecimal.valueOf(1000*0.102*43).setScale(0, RoundingMode.HALF_UP)); //1000 dagsats, 10.2% sats, 44 dager
        assertThat(periode1feriepenger2019).isEqualByComparingTo(BigDecimal.valueOf(1000*0.102*4).setScale(0, RoundingMode.HALF_UP)); //som over, men 3 dager
        assertThat(periode3feriepengerBruker2019).isEqualByComparingTo(BigDecimal.valueOf(500*0.102*56).setScale(0, RoundingMode.HALF_UP)); //500 dagsats, sats, 56 dager (kvote nullstilles ved årskifte, 4 dager brukt i forrige periode)
        assertThat(periode3feriepengerRefusjon2019).isEqualByComparingTo(BigDecimal.valueOf(500*0.102*56).setScale(0, RoundingMode.HALF_UP));
    }

    @Test
    void skal_beregne_feriepenger_for_ytelse_i_helger_når_det_er_valgt__dette_brukes_for_omsorgspenger() {
        LocalDate mandag = LocalDate.of(2021, 1, 11);
        LocalDate fredag = LocalDate.of(2021, 1, 15);
        LocalDate lørdag = LocalDate.of(2021, 1, 16);
        LocalDate søndag = LocalDate.of(2021, 1, 17);
        BeregningsresultatPeriode periode1 = new BeregningsresultatPeriode(lørdag, lørdag);
        BeregningsresultatPeriode periode2 = new BeregningsresultatPeriode(søndag.plusWeeks(1), mandag.plusWeeks(2));
        BeregningsresultatPeriode periode3 = new BeregningsresultatPeriode(fredag.plusWeeks(2), lørdag.plusWeeks(2));
        byggAndelerForPeriode(periode1, 100, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode2, 200, 0, arbeidsforhold1);
        byggAndelerForPeriode(periode3, 300, 0, arbeidsforhold1);

        BeregningsresultatFeriepengerRegelModell regelModell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(periode1, periode2, periode3))
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medAntallDagerFeriepenger(60)
            .medFeriepengeopptjeningForHelg(true)
            .medAndelerSomKanGiFeriepengerForRelevaneSaker(LocalDateTimeline.empty())
            .build();

        RegelBeregnFeriepenger regel = new RegelBeregnFeriepenger();
        Evaluation evaluation = regel.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();

        List<BeregningsresultatPeriode> perioder = regelModell.getBeregningsresultatPerioder();
        perioder.get(0).getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        perioder.get(1).getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        perioder.get(2).getBeregningsresultatAndelList().forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        BeregningsresultatAndel andelBruker1 = periode1.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel andelBruker2 = periode2.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel andelBruker3 = periode3.getBeregningsresultatAndelList().get(0);

        assertThat(andelBruker1.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(new BigDecimal("10")); //10.2 % av 100 * 1 dag, avrundet
        assertThat(andelBruker2.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(new BigDecimal("41"));//10.2 % av 200 * 2 dager, avrundet
        assertThat(andelBruker3.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(new BigDecimal("61"));//10.2 % av 300 * 2 dager, avrundet
    }

    @Test
    void skal_beregne_feriepenger_for_refusjon_når_det_er_valgt_men_begrenset_antall_dager_for_utbetaling_til_bruker() {
        //bakgrunn: det finnes ikke noen hjemmel for å begrense opptjeningsdager ved refusjon for omsorgspenger. Tidligere praksis er å alltid utbetale.
        LocalDate fom = LocalDate.of(2021, 1, 1);
        LocalDate tom = fom.plusDays(99);
        BeregningsresultatPeriode brPeriode = new BeregningsresultatPeriode(fom, tom);
        byggAndelerForPeriode(brPeriode, 200, 200, arbeidsforhold1);

        BeregningsresultatFeriepengerRegelModell regelModell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(brPeriode))
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medAntallDagerFeriepenger(60)
            .medFeriepengeopptjeningForHelg(true)
            .medUbegrensetFeriepengedagerVedRefusjon(true)
            .medAndelerSomKanGiFeriepengerForRelevaneSaker(LocalDateTimeline.empty())
            .build();

        RegelBeregnFeriepenger regel = new RegelBeregnFeriepenger();
        Evaluation evaluation = regel.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();

        List<BeregningsresultatPeriode> perioder = regelModell.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(1);
        List<BeregningsresultatAndel> brAndeler = perioder.get(0).getBeregningsresultatAndelList();
        assertThat(brAndeler).hasSize(2);
        brAndeler.forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        BeregningsresultatAndel andelBruker = brAndeler.stream().filter(a -> a.erBrukerMottaker()).findFirst().orElseThrow();
        BeregningsresultatAndel andelArbeidsgiver = brAndeler.stream().filter(a -> !a.erBrukerMottaker()).findFirst().orElseThrow();

        assertThat(andelBruker.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(1224)); //10.2 % av 200 kr * 60 dager (dager kappes)
        assertThat(andelArbeidsgiver.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(2040)); //10.2 % av 200 kr * 100 dager
    }

    @Test
    void skal_ikke_trekke_dager_fra_brukes_feriepengedagerkvote_for_dager_med_kun_refusjon_når_det_er_ubegrenset_for_arbeidsgiver() {
        LocalDate fom1 = LocalDate.of(2021, 1, 1);
        LocalDate tom1 = fom1.plusDays(99);
        LocalDate fom2 = tom1.plusDays(1);
        LocalDate tom2 = fom2.plusDays(50);
        BeregningsresultatPeriode periodeKunRefusjon = new BeregningsresultatPeriode(fom1, tom1);
        byggAndelerForPeriode(periodeKunRefusjon, 0, 200, arbeidsforhold1);
        BeregningsresultatPeriode periodeKunBruker = new BeregningsresultatPeriode(fom2, tom2);
        byggAndelerForPeriode(periodeKunBruker, 100, 0, arbeidsforhold1);

        BeregningsresultatFeriepengerRegelModell regelModell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(periodeKunRefusjon, periodeKunBruker))
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medAntallDagerFeriepenger(48)
            .medFeriepengeopptjeningForHelg(true)
            .medUbegrensetFeriepengedagerVedRefusjon(true)
            .medAndelerSomKanGiFeriepengerForRelevaneSaker(LocalDateTimeline.empty())
            .build();

        RegelBeregnFeriepenger regel = new RegelBeregnFeriepenger();
        Evaluation evaluation = regel.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();

        List<BeregningsresultatPeriode> perioder = regelModell.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(2);
        BeregningsresultatPeriode periode2 = perioder.get(1);
        List<BeregningsresultatAndel> brAndeler2 = periode2.getBeregningsresultatAndelList();
        assertThat(brAndeler2).hasSize(1);
        brAndeler2.forEach(andel -> assertThat(andel.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1));
        BeregningsresultatAndel andelBruker = brAndeler2.stream().filter(BeregningsresultatAndel::erBrukerMottaker).findFirst().orElseThrow();
        assertThat(andelBruker.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(new BigDecimal("490")); //10.2 % av 100 kr * 48 dager (dager kappes), avrudet
    }

    @Test
    void skal_beregne_feriepenger_når_det_er_innvilget_utelukkende_refusjon() {
        LocalDate fom = LocalDate.of(2021, 1, 1);
        LocalDate tom = fom;
        BeregningsresultatPeriode brPeriode = new BeregningsresultatPeriode(fom, tom);
        byggAndelerForPeriode(brPeriode, 0, 1000, arbeidsforhold1);

        BeregningsresultatFeriepengerRegelModell regelModell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(List.of(brPeriode))
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medAntallDagerFeriepenger(60)
            .medFeriepengeopptjeningForHelg(true)
            .medUbegrensetFeriepengedagerVedRefusjon(true)
            .medAndelerSomKanGiFeriepengerForRelevaneSaker(LocalDateTimeline.empty())
            .build();

        RegelBeregnFeriepenger regel = new RegelBeregnFeriepenger();
        Evaluation evaluation = regel.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();

        List<BeregningsresultatPeriode> perioder = regelModell.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(1);
        List<BeregningsresultatAndel> brAndeler = perioder.get(0).getBeregningsresultatAndelList();
        assertThat(brAndeler).hasSize(2);
        BeregningsresultatAndel andelBruker = brAndeler.stream().filter(a -> a.erBrukerMottaker()).findFirst().orElseThrow();
        BeregningsresultatAndel andelArbeidsgiver = brAndeler.stream().filter(a -> !a.erBrukerMottaker()).findFirst().orElseThrow();

        assertThat(andelBruker.getBeregningsresultatFeriepengerPrÅrListe().isEmpty());
        assertThat(andelArbeidsgiver.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1);
        assertThat(andelArbeidsgiver.getBeregningsresultatFeriepengerPrÅrListe().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(102)); //10.2 % av 1000 kr * 1 dag
    }

    @Test
    void skal_beregne_feriepenger_når_beregningsresultat_er_tomt() {
        BeregningsresultatFeriepengerRegelModell regelModell = BeregningsresultatFeriepengerRegelModell.builder()
            .medBeregningsresultatPerioder(Collections.emptyList())
            .medInntektskategorier(Collections.singleton(Inntektskategori.ARBEIDSTAKER))
            .medAntallDagerFeriepenger(60)
            .medFeriepengeopptjeningForHelg(true)
            .medUbegrensetFeriepengedagerVedRefusjon(true)
            .medAndelerSomKanGiFeriepengerForRelevaneSaker(LocalDateTimeline.empty())
            .build();

        RegelBeregnFeriepenger regel = new RegelBeregnFeriepenger();
        Evaluation evaluation = regel.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        assertThat(sporing).isNotNull();
        assertThat(regelModell.getBeregningsresultatPerioder()).isEmpty();
    }

    private void byggAndelerForPeriode(BeregningsresultatPeriode periode, int dagsats, int refusjon, Arbeidsforhold arbeidsforhold1) {
        BeregningsresultatAndel.builder()
            .medDagsats((long) dagsats)
            .medDagsatsFraBg((long) dagsats)
            .medAktivitetStatus(AktivitetStatus.ATFL)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBrukerErMottaker(true)
            .medArbeidsforhold(arbeidsforhold1)
            .build(periode);
        if (refusjon > 0) {
            BeregningsresultatAndel.builder()
                .medDagsats((long) refusjon)
                .medDagsatsFraBg((long) refusjon)
                .medAktivitetStatus(AktivitetStatus.ATFL)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medBrukerErMottaker(false)
                .medArbeidsforhold(arbeidsforhold1)
                .build(periode);
        }
    }


}
