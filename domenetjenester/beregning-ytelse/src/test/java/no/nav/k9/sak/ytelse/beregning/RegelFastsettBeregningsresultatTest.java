package no.nav.k9.sak.ytelse.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Periode;
import no.nav.k9.sak.ytelse.beregning.regler.RegelFastsettBeregningsresultat;

public class RegelFastsettBeregningsresultatTest {
    private static final LocalDate TRE_UKER_FØR_FØDSEL_DT = LocalDate.now().minusWeeks(3);
    private static final LocalDate FØDSELSDATO = LocalDate.now();
    private static final LocalDate DAGEN_ETTER_FØDSEL = LocalDate.now().plusDays(1);
    private static final LocalDate TI_UKER_ETTER_FØDSEL_DT = LocalDate.now().plusWeeks(10);
    private static final LocalDate FØRTI_SEKS_UKER_ETTER_FØDSEL_DT = LocalDate.now().plusWeeks(46);
    private static final LocalDateInterval FELLESPERIODE_FØR_FØDSEL = new LocalDateInterval(TRE_UKER_FØR_FØDSEL_DT, FØDSELSDATO);
    private static final LocalDateInterval MØDREKVOTE_PERIODE = new LocalDateInterval(DAGEN_ETTER_FØDSEL, TI_UKER_ETTER_FØDSEL_DT);
    private static final LocalDateInterval FELLESPERIODE = new LocalDateInterval(TI_UKER_ETTER_FØDSEL_DT.plusDays(1), FØRTI_SEKS_UKER_ETTER_FØDSEL_DT);
    private static final LocalDateInterval BG_PERIODE_1 = new LocalDateInterval(TRE_UKER_FØR_FØDSEL_DT, FØDSELSDATO.plusWeeks(4));
    private static final LocalDateInterval BG_PERIODE_2 = new LocalDateInterval(DAGEN_ETTER_FØDSEL.plusWeeks(4), LocalDate.MAX);
    private static final Arbeidsforhold ANONYMT_ARBEIDSFORHOLD = null;
    private static final Arbeidsforhold ARBEIDSFORHOLD_1 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("111", InternArbeidsforholdRef.nyRef());
    private static final Arbeidsforhold ARBEIDSFORHOLD_2 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("222", InternArbeidsforholdRef.nyRef());
    private static final Arbeidsforhold ARBEIDSFORHOLD_3 = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet("333", InternArbeidsforholdRef.nyRef());

    private RegelFastsettBeregningsresultat regel;

    private final FagsakYtelseType ytelseType =FagsakYtelseType.OMSORGSPENGER;

    @Before
    public void setup() {
        regel = new RegelFastsettBeregningsresultat();
    }

    @Test
    public void skalLageAndelForBrukerOgArbeidsgiverForEnPeriode() {
        // Arrange
        BeregningsresultatRegelmodell modell = opprettRegelmodellEnPeriode();
        Beregningsresultat output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        List<BeregningsresultatPeriode> perioder = output.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(1);
        BeregningsresultatPeriode periode = perioder.get(0);
        assertThat(periode.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);
        List<BeregningsresultatAndel> andelList = periode.getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(2);

        List<BeregningsresultatAndel> brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker).collect(Collectors.toList());
        List<BeregningsresultatAndel> arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).collect(Collectors.toList());
        assertThat(brukerAndeler).hasSize(1);
        assertThat(brukerAndeler.get(0).getArbeidsforhold().getIdentifikator()).isEqualTo("111");
        assertThat(brukerAndeler.get(0).getDagsats()).isEqualTo(1000);

        assertThat(arbAndeler).hasSize(1);
        assertThat(arbAndeler.get(0).getArbeidsforhold().getIdentifikator()).isEqualTo("111");
        assertThat(arbAndeler.get(0).getDagsats()).isEqualTo(1000);
    }

    @Test
    public void skalPeriodisereFlereUttaksPerioder() {
        // Arrange
        List<LocalDateInterval> intervalList = List.of(
            FELLESPERIODE_FØR_FØDSEL,
            MØDREKVOTE_PERIODE
        );
        BeregningsresultatRegelmodell modell = opprettRegelmodell(intervalList, AktivitetStatus.ATFL, UttakArbeidType.ARBEIDSTAKER);
        Beregningsresultat output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        List<BeregningsresultatPeriode> perioder = output.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(2);
        BeregningsresultatPeriode periode0 = perioder.get(0);
        assertThat(periode0.getFom()).isEqualTo(TRE_UKER_FØR_FØDSEL_DT);
        assertThat(periode0.getTom()).isEqualTo(FØDSELSDATO);

        BeregningsresultatPeriode periode1 = perioder.get(1);
        assertThat(periode1.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode1.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);
    }

    @Test
    public void skalLageAndelerForFlereArbeidsforhold() {
        // Arrange
        BeregningsgrunnlagPrArbeidsforhold arb1 = lagPrArbeidsforhold(2000.0, 0.0, ARBEIDSFORHOLD_1);
        BeregningsgrunnlagPrArbeidsforhold arb2 = lagPrArbeidsforhold(0.0, 1500.0, ARBEIDSFORHOLD_2);
        BeregningsgrunnlagPrArbeidsforhold arb3 = lagPrArbeidsforhold(1000.0, 500.0, ARBEIDSFORHOLD_3);

        BeregningsresultatRegelmodell modell = opprettRegelmodellMedArbeidsforhold(arb1, arb2, arb3);
        Beregningsresultat output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        List<BeregningsresultatPeriode> perioder = output.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(1);
        BeregningsresultatPeriode periode = perioder.get(0);
        assertThat(periode.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);

        List<BeregningsresultatAndel> andelList = periode.getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(5);

        List<BeregningsresultatAndel> brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker).collect(Collectors.toList());
        List<BeregningsresultatAndel> arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).collect(Collectors.toList());
        assertThat(brukerAndeler).hasSize(3);

        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("111")).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(2000);
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("222")).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(0);
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("333")).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(1000);

        assertThat(arbAndeler).hasSize(2);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("222")).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(1500);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("333")).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(500);
    }

    @Test
    public void skalLageAndelerForAnonymtArbeidsforhold() {
        // Arrange
        BeregningsgrunnlagPrArbeidsforhold arb1 = lagPrArbeidsforhold(2000.0, 0.0, ANONYMT_ARBEIDSFORHOLD);
        BeregningsgrunnlagPrArbeidsforhold arb2 = lagPrArbeidsforhold(0.0, 1500.0, ARBEIDSFORHOLD_2);

        BeregningsresultatRegelmodell modell = opprettRegelmodellMedArbeidsforhold(arb1, arb2);
        Beregningsresultat output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        List<BeregningsresultatPeriode> perioder = output.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(1);
        BeregningsresultatPeriode periode = perioder.get(0);
        assertThat(periode.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode.getTom()).isEqualTo(TI_UKER_ETTER_FØDSEL_DT);

        List<BeregningsresultatAndel> andelList = periode.getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(3);

        List<BeregningsresultatAndel> brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker).collect(Collectors.toList());
        List<BeregningsresultatAndel> arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).collect(Collectors.toList());
        assertThat(brukerAndeler).hasSize(2);

        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold() == null).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(2000);
        assertThat(brukerAndeler.stream().filter(af -> "222".equals(af.getArbeidsgiverId())).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(0);

        assertThat(arbAndeler).hasSize(1);
        assertThat(arbAndeler.stream().filter(af -> "222".equals(af.getArbeidsforhold().getIdentifikator())).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(1500);
    }

    @Test
    public void skalPeriodisereFlereUttaksPerioderOgBeregningsgrunnlagPerioder() {
        // Arrange
        BeregningsresultatRegelmodell modell = opprettRegelmodellMedFlereBGOgUttakPerioder();
        Beregningsresultat output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        List<BeregningsresultatPeriode> perioder = output.getBeregningsresultatPerioder();
        assertThat(perioder).hasSize(4);

        BeregningsresultatPeriode periode1 = perioder.get(0);
        assertThat(periode1.getFom()).isEqualTo(TRE_UKER_FØR_FØDSEL_DT);
        assertThat(periode1.getTom()).isEqualTo(FØDSELSDATO);
        assertThat(periode1.getBeregningsresultatAndelList()).hasSize(2);
        assertThat(periode1.getBeregningsresultatAndelList().stream().filter(BeregningsresultatAndel::erBrukerMottaker)
            .collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(1000);
        assertThat(periode1.getBeregningsresultatAndelList().stream().filter(a -> !a.erBrukerMottaker())
            .collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(800);

        BeregningsresultatPeriode periode2 = perioder.get(1);
        assertThat(periode2.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL);
        assertThat(periode2.getTom()).isEqualTo(FØDSELSDATO.plusWeeks(4));
        assertThat(periode2.getBeregningsresultatAndelList()).hasSize(2);
        assertThat(periode2.getBeregningsresultatAndelList().stream().filter(BeregningsresultatAndel::erBrukerMottaker)
            .collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(1000);
        assertThat(periode2.getBeregningsresultatAndelList().stream().filter(a -> !a.erBrukerMottaker())
            .collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(800);

        BeregningsresultatPeriode periode3 = perioder.get(2);
        assertThat(periode3.getFom()).isEqualTo(DAGEN_ETTER_FØDSEL.plusWeeks(4));
        assertThat(periode3.getTom()).isEqualTo(MØDREKVOTE_PERIODE.getTomDato());
        assertThat(periode3.getBeregningsresultatAndelList()).hasSize(1);
        assertThat(periode3.getBeregningsresultatAndelList().get(0).getDagsats()).isEqualTo(2000);
        assertThat(periode3.getBeregningsresultatAndelList().get(0).erBrukerMottaker()).isTrue();

        BeregningsresultatPeriode periode4 = perioder.get(3);
        assertThat(periode4.getFom()).isEqualTo(FELLESPERIODE.getFomDato());
        assertThat(periode4.getTom()).isEqualTo(FELLESPERIODE.getTomDato());
        assertThat(periode4.getBeregningsresultatAndelList()).hasSize(1);
        assertThat(periode4.getBeregningsresultatAndelList().get(0).getDagsats()).isEqualTo(2000);
        assertThat(periode3.getBeregningsresultatAndelList().get(0).erBrukerMottaker()).isTrue();
    }

    @Test
    public void skalRundeAvAndelerRiktig() {
        // Arrange
        BeregningsgrunnlagPrArbeidsforhold arb1 = lagPrArbeidsforhold(2165.49, 0.00, ARBEIDSFORHOLD_1);
        BeregningsgrunnlagPrArbeidsforhold arb2 = lagPrArbeidsforhold(0.455, 1550.50, ARBEIDSFORHOLD_2);
        BeregningsgrunnlagPrArbeidsforhold arb3 = lagPrArbeidsforhold(1001.50, 500.49, ARBEIDSFORHOLD_3);

        BeregningsresultatRegelmodell modell = opprettRegelmodellMedArbeidsforhold(arb1, arb2, arb3);
        Beregningsresultat output = new Beregningsresultat();

        // Act
        regel.evaluer(modell, output);

        // Assert
        List<BeregningsresultatAndel> andelList = output.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andelList).hasSize(5);

        List<BeregningsresultatAndel> brukerAndeler = andelList.stream().filter(BeregningsresultatAndel::erBrukerMottaker).collect(Collectors.toList());
        List<BeregningsresultatAndel> arbAndeler = andelList.stream().filter(a -> !a.erBrukerMottaker()).collect(Collectors.toList());
        assertThat(brukerAndeler).hasSize(3);

        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("111")).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(2165);
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("222")).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(0);
        assertThat(brukerAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("333")).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(1002);

        assertThat(arbAndeler).hasSize(2);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("222")).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(1551);
        assertThat(arbAndeler.stream().filter(af -> af.getArbeidsforhold().getIdentifikator().equals("333")).collect(Collectors.toList()).get(0).getDagsats()).isEqualTo(500);
    }


    private BeregningsresultatRegelmodell opprettRegelmodellEnPeriode() {
        List<LocalDateInterval> perioder = Collections.singletonList(MØDREKVOTE_PERIODE);
        return opprettRegelmodell(perioder, AktivitetStatus.ATFL, UttakArbeidType.ARBEIDSTAKER);
    }

    private BeregningsresultatRegelmodell opprettRegelmodell(List<LocalDateInterval> perioder, AktivitetStatus aktivitetsStatus, UttakArbeidType uttakArbeidType) {
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlag();
        UttakResultat uttakResultat = opprettUttak(perioder, aktivitetsStatus, uttakArbeidType, Collections.emptyList());
        return new BeregningsresultatRegelmodell(beregningsgrunnlag, uttakResultat);
    }

    private BeregningsresultatRegelmodell opprettRegelmodellMedFlereBGOgUttakPerioder() {
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlagForFlerePerioder();
        List<LocalDateInterval> uttakPerioder = List.of(
            FELLESPERIODE_FØR_FØDSEL,
            MØDREKVOTE_PERIODE,
            FELLESPERIODE
        );
        List<Arbeidsforhold> arbeidsforholdList = List.of(ARBEIDSFORHOLD_1, ARBEIDSFORHOLD_2);
        UttakResultat uttakResultat = opprettUttak(uttakPerioder, AktivitetStatus.ATFL, UttakArbeidType.ARBEIDSTAKER, arbeidsforholdList);
        return new BeregningsresultatRegelmodell(beregningsgrunnlag, uttakResultat);
    }

    private BeregningsresultatRegelmodell opprettRegelmodellMedArbeidsforhold(BeregningsgrunnlagPrArbeidsforhold... arbeidsforhold) {
        Beregningsgrunnlag beregningsgrunnlag = opprettBeregningsgrunnlag(arbeidsforhold);
        List<Arbeidsforhold> arbeidsforholdList = Arrays.stream(arbeidsforhold).map(BeregningsgrunnlagPrArbeidsforhold::getArbeidsforhold).collect(Collectors.toList());
        UttakResultat uttakResultat = opprettUttak(Collections.singletonList(MØDREKVOTE_PERIODE), AktivitetStatus.ATFL, UttakArbeidType.ARBEIDSTAKER, arbeidsforholdList);
        return new BeregningsresultatRegelmodell(beregningsgrunnlag, uttakResultat);
    }

    private BeregningsgrunnlagPrArbeidsforhold lagPrArbeidsforhold(double dagsatsBruker, double dagsatsArbeidsgiver, Arbeidsforhold arbeidsforhold) {
        return BeregningsgrunnlagPrArbeidsforhold.builder()
            .medArbeidsforhold(arbeidsforhold)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medRedusertRefusjonPrÅr(BigDecimal.valueOf(260 * dagsatsArbeidsgiver))
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(260 * dagsatsBruker))
            .build();
    }

    private Beregningsgrunnlag opprettBeregningsgrunnlag(BeregningsgrunnlagPrArbeidsforhold... ekstraArbeidsforhold) {
        BeregningsgrunnlagPrStatus.Builder prStatusBuilder = BeregningsgrunnlagPrStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ATFL);
        if (ekstraArbeidsforhold.length == 0) {
            prStatusBuilder.medArbeidsforhold(lagPrArbeidsforhold(1000.0, 1000.0, ARBEIDSFORHOLD_1));
        }
        for (BeregningsgrunnlagPrArbeidsforhold arbeidsforhold : ekstraArbeidsforhold) {
            prStatusBuilder.medArbeidsforhold(arbeidsforhold);
        }

        BeregningsgrunnlagPrStatus prStatus = prStatusBuilder.build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medPeriode(Periode.of(TRE_UKER_FØR_FØDSEL_DT, LocalDate.MAX))
            .medBeregningsgrunnlagPrStatus(prStatus)
            .build();
        return Beregningsgrunnlag.builder()
            .medAktivitetStatuser(Collections.singletonList(AktivitetStatus.ATFL))
            .medSkjæringstidspunkt(LocalDate.now())
            .medBeregningsgrunnlagPeriode(periode)
            .build();
    }

    private Beregningsgrunnlag opprettBeregningsgrunnlagForFlerePerioder() {

        BeregningsgrunnlagPrStatus prStatus1 = BeregningsgrunnlagPrStatus.builder().medAktivitetStatus(AktivitetStatus.ATFL)
            .medArbeidsforhold(lagPrArbeidsforhold(1000.0, 800.0, ARBEIDSFORHOLD_1)).build();
        BeregningsgrunnlagPrStatus prStatus2 = BeregningsgrunnlagPrStatus.builder().medAktivitetStatus(AktivitetStatus.ATFL)
            .medArbeidsforhold(lagPrArbeidsforhold(2000.0, 0.0, ARBEIDSFORHOLD_2)).build();

        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medPeriode(Periode.of(BG_PERIODE_1.getFomDato(), BG_PERIODE_1.getTomDato()))
            .medBeregningsgrunnlagPrStatus(prStatus1)
            .build();

        BeregningsgrunnlagPeriode periode2 = BeregningsgrunnlagPeriode.builder()
            .medPeriode(Periode.of(BG_PERIODE_2.getFomDato(), BG_PERIODE_2.getTomDato()))
            .medBeregningsgrunnlagPrStatus(prStatus2)
            .build();

        return Beregningsgrunnlag.builder()
            .medAktivitetStatuser(Collections.singletonList(AktivitetStatus.ATFL))
            .medSkjæringstidspunkt(LocalDate.now())
            .medBeregningsgrunnlagPeriode(periode1)
            .medBeregningsgrunnlagPeriode(periode2)
            .build();
    }

    private UttakResultat opprettUttak(List<LocalDateInterval> perioder, AktivitetStatus aktivitetsStatus, UttakArbeidType uttakArbeidType, List<Arbeidsforhold> arbeidsforhold) {
        List<UttakResultatPeriode> periodeListe = new ArrayList<>();
        for (LocalDateInterval periode : perioder) {
            List<UttakAktivitet> uttakAktiviteter = lagUttakAktiviteter(BigDecimal.valueOf(100), BigDecimal.valueOf(100), aktivitetsStatus, uttakArbeidType, arbeidsforhold);
            periodeListe.add(new UttakResultatPeriode(periode.getFomDato(), periode.getTomDato(), uttakAktiviteter, false));
        }
        return new UttakResultat(ytelseType, periodeListe);
    }

    private List<UttakAktivitet> lagUttakAktiviteter(BigDecimal stillingsgrad, BigDecimal utbetalingsgrad, AktivitetStatus aktivitetsStatus, UttakArbeidType uttakArbeidType, List<Arbeidsforhold> arbeidsforholdList) {
        boolean erGradering = false;
        if (arbeidsforholdList.isEmpty()) {
            return Collections.singletonList(new UttakAktivitet(stillingsgrad, utbetalingsgrad, aktivitetsStatus.equals(AktivitetStatus.ATFL) ? ARBEIDSFORHOLD_1 : null, uttakArbeidType, erGradering));
        }
        return arbeidsforholdList.stream()
            .map(arb ->
            {
                Arbeidsforhold arbeidsforhold = aktivitetsStatus.equals(AktivitetStatus.ATFL) ? arb : null;
                return new UttakAktivitet(stillingsgrad, utbetalingsgrad, arbeidsforhold, uttakArbeidType, erGradering);
            }).collect(Collectors.toList());
    }
}
