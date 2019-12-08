package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste.VurderManuellBehandling;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.PeriodeÅrsak;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FordelBeregningsgrunnlagArbeidsforholdDto;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjenesteImpl;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.tid.DatoIntervallEntitet;

public class RefusjonEllerGraderingArbeidsforholdDtoTjenesteTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.MAY, 10);
    public static final String ORGNR = "7238947234423";
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.namedRef("TEST-REF");

    private final Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet(ORGNR);

    private final FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste = mock(FordelBeregningsgrunnlagTjeneste.class);
    private final BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = mock(BeregningAktivitetAggregatEntitet.class);

    private RefusjonEllerGraderingArbeidsforholdDtoTjeneste arbeidsforholdDtoTjeneste;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = mock(BeregningsgrunnlagRepository.class);
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private BeregningsgrunnlagPrStatusOgAndel arbeidstakerAndel;
    private BGAndelArbeidsforhold.Builder arbeidsforholdBuilder;
    private BeregningsgrunnlagPeriode periode;
    private BehandlingReferanse referanse;
    private BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet;

    @Before
    public void setUp() {
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        referanse = scenario.lagMocked();
        referanse = referanse.medSkjæringstidspunkt(Skjæringstidspunkt.builder().medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING).build());
        when(beregningAktivitetAggregat.getBeregningAktiviteter()).thenReturn(Collections.emptyList());
        var virksomhetBuilder = new VirksomhetEntitet.Builder();
        var virksomhet = virksomhetBuilder.medOrgnr(ORGNR).build();
        when(refusjonOgGraderingTjeneste.vurderManuellBehandlingForAndel(any(), any(), any(), any(), any()))
            .thenReturn(Optional.of(VurderManuellBehandling.NYTT_ARBEIDSFORHOLD));

        var virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentVirksomhet(any())).thenReturn(Optional.of(virksomhet));
        when(virksomhetTjeneste.hentOgLagreOrganisasjon(any())).thenReturn(virksomhet);
        ArbeidsgiverTjeneste arbeidsgiverTjeneste = new ArbeidsgiverTjenesteImpl(null, virksomhetTjeneste);
        BeregningsgrunnlagDtoUtil dtoUtil = new BeregningsgrunnlagDtoUtil(beregningsgrunnlagRepository, arbeidsgiverTjeneste);
        arbeidsforholdDtoTjeneste = new RefusjonEllerGraderingArbeidsforholdDtoTjeneste(refusjonOgGraderingTjeneste, dtoUtil);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING).medGrunnbeløp(BigDecimal.valueOf(600000))
            .build();
        periode = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_OPPTJENING, null)
            .build(beregningsgrunnlag);
        arbeidsforholdBuilder = BGAndelArbeidsforhold
            .builder()
            .medArbeidsgiver(arbeidsgiver1)
            .medArbeidsforholdRef(ARBEIDSFORHOLD_REF);
        arbeidstakerAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(arbeidsforholdBuilder)
            .build(periode);
        grunnlagEntitet = mock(BeregningsgrunnlagGrunnlagEntitet.class);
        when(grunnlagEntitet.getBeregningsgrunnlag()).thenReturn(Optional.of(beregningsgrunnlag));
        when(beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(any(), any())).thenReturn(Optional.of(grunnlagEntitet));
    }

    @Test
    public void skal_ikkje_lage_arbeidsforhold_dto_om_ingen_refusjon_eller_gradering() {
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var input = new BeregningsgrunnlagInput(referanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(grunnlagEntitet);
        List<FordelBeregningsgrunnlagArbeidsforholdDto> listeMedGraderingRefusjonDto = arbeidsforholdDtoTjeneste
            .lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(input);
        assertThat(listeMedGraderingRefusjonDto).isEmpty();
    }

    // Periode 1:
    // Søker refusjon lik 10
    @Test
    public void skal_lage_dto_med_refusjon_for_refusjon_i_heile_perioden() {
        BeregningsgrunnlagPrStatusOgAndel.builder(arbeidstakerAndel).medBGAndelArbeidsforhold(arbeidsforholdBuilder.medRefusjonskravPrÅr(BigDecimal.TEN));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var input = new BeregningsgrunnlagInput(referanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(grunnlagEntitet);

        List<FordelBeregningsgrunnlagArbeidsforholdDto> listeMedGraderingRefusjonDto = arbeidsforholdDtoTjeneste
            .lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(input);
        assertThat(listeMedGraderingRefusjonDto).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon()).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErRefusjon()).isTrue();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErGradering()).isFalse();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getFom())
            .isEqualTo(periode.getBeregningsgrunnlagPeriodeFom());
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getTom()).isNull();
    }

    // Periode 1:
    // Søker refusjon lik 10
    // Periode 2:
    // Opphører refusjon
    @Test
    public void skal_lage_dto_med_refusjon_for_refusjon_med_opphør() {
        BeregningsgrunnlagPrStatusOgAndel andelIAndrePeriode = Kopimaskin.deepCopy(arbeidstakerAndel);
        LocalDate andrePeriodeFom = periode.getBeregningsgrunnlagPeriodeFom().plusMonths(1);
        BeregningsgrunnlagPeriode andrePeriode = BeregningsgrunnlagPeriode.builder()
            .leggTilPeriodeÅrsak(PeriodeÅrsak.REFUSJON_OPPHØRER)
            .medBeregningsgrunnlagPeriode(andrePeriodeFom, null).build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder(andelIAndrePeriode).build(andrePeriode);
        BeregningsgrunnlagPeriode.builder(periode).medBeregningsgrunnlagPeriode(periode.getBeregningsgrunnlagPeriodeFom(), andrePeriodeFom.minusDays(1));
        BeregningsgrunnlagPrStatusOgAndel.builder(arbeidstakerAndel).medBGAndelArbeidsforhold(arbeidsforholdBuilder.medRefusjonskravPrÅr(BigDecimal.TEN));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var input = new BeregningsgrunnlagInput(referanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(grunnlagEntitet);

        List<FordelBeregningsgrunnlagArbeidsforholdDto> listeMedGraderingRefusjonDto = arbeidsforholdDtoTjeneste
            .lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(input);
        assertThat(listeMedGraderingRefusjonDto).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon()).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErRefusjon()).isTrue();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErGradering()).isFalse();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getFom())
            .isEqualTo(periode.getBeregningsgrunnlagPeriodeFom());
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getTom())
            .isEqualTo(periode.getBeregningsgrunnlagPeriodeTom());
    }

    // Periode 1:
    // Søker ikke refusjon
    // Periode 2:
    // Søker refusjon lik 10
    @Test
    public void skal_lage_dto_med_refusjon_for_tilkommet_refusjon() {
        BeregningsgrunnlagPrStatusOgAndel andelIAndrePeriode = Kopimaskin.deepCopy(arbeidstakerAndel);
        LocalDate andrePeriodeFom = periode.getBeregningsgrunnlagPeriodeFom().plusMonths(1);
        BeregningsgrunnlagPeriode andrePeriode = BeregningsgrunnlagPeriode.builder()
            .leggTilPeriodeÅrsak(PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV)
            .medBeregningsgrunnlagPeriode(andrePeriodeFom, null).build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder(andelIAndrePeriode)
            .medBGAndelArbeidsforhold(arbeidsforholdBuilder.medRefusjonskravPrÅr(BigDecimal.TEN))
            .build(andrePeriode);
        BeregningsgrunnlagPeriode.builder(periode).medBeregningsgrunnlagPeriode(periode.getBeregningsgrunnlagPeriodeFom(), andrePeriodeFom.minusDays(1));
        BeregningsgrunnlagPrStatusOgAndel.builder(arbeidstakerAndel)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdRef(ARBEIDSFORHOLD_REF));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var input = new BeregningsgrunnlagInput(referanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(grunnlagEntitet);

        List<FordelBeregningsgrunnlagArbeidsforholdDto> listeMedGraderingRefusjonDto = arbeidsforholdDtoTjeneste
            .lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(input);
        assertThat(listeMedGraderingRefusjonDto).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon()).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErRefusjon()).isTrue();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErGradering()).isFalse();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getFom())
            .isEqualTo(andrePeriode.getBeregningsgrunnlagPeriodeFom());
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getTom()).isNull();
    }

    // Periode 1:
    // Søker refusjon lik 1
    // Periode 2:
    // Søker refusjon lik 10
    @Test
    public void skal_lage_dto_med_refusjon_for_endret_refusjon() {
        BeregningsgrunnlagPrStatusOgAndel andelIAndrePeriode = Kopimaskin.deepCopy(arbeidstakerAndel);
        LocalDate andrePeriodeFom = periode.getBeregningsgrunnlagPeriodeFom().plusMonths(1);
        BeregningsgrunnlagPeriode andrePeriode = BeregningsgrunnlagPeriode.builder()
            .leggTilPeriodeÅrsak(PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV)
            .medBeregningsgrunnlagPeriode(andrePeriodeFom, null).build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder(andelIAndrePeriode)
            .medBGAndelArbeidsforhold(arbeidsforholdBuilder.medRefusjonskravPrÅr(BigDecimal.TEN))
            .build(andrePeriode);
        BeregningsgrunnlagPeriode.builder(periode).medBeregningsgrunnlagPeriode(periode.getBeregningsgrunnlagPeriodeFom(), andrePeriodeFom.minusDays(1));
        BeregningsgrunnlagPrStatusOgAndel.builder(arbeidstakerAndel)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdRef(ARBEIDSFORHOLD_REF).medRefusjonskravPrÅr(BigDecimal.ONE));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var input = new BeregningsgrunnlagInput(referanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(grunnlagEntitet);

        List<FordelBeregningsgrunnlagArbeidsforholdDto> listeMedGraderingRefusjonDto = arbeidsforholdDtoTjeneste
            .lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(input);
        assertThat(listeMedGraderingRefusjonDto).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon()).hasSize(2);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErRefusjon()).isTrue();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErGradering()).isFalse();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getFom())
            .isEqualTo(periode.getBeregningsgrunnlagPeriodeFom());
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getTom())
            .isEqualTo(periode.getBeregningsgrunnlagPeriodeTom());

        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(1).isErRefusjon()).isTrue();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(1).isErGradering()).isFalse();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(1).getFom())
            .isEqualTo(andrePeriode.getBeregningsgrunnlagPeriodeFom());
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(1).getTom()).isNull();
    }

    // Periode 1:
    // Søker refusjon lik 1
    // Periode 2:
    // Opphører refusjon
    // Periode 3:
    // Søker refusjon lik 10
    @Test
    public void skal_lage_dto_med_refusjon_for_endret_refusjon_med_opphør_mellom_perioder() {
        // Arrange

        // Tredje periode (endring i refusjon)
        BeregningsgrunnlagPrStatusOgAndel andelITredjePeriode = Kopimaskin.deepCopy(arbeidstakerAndel);
        LocalDate tredjePeriodeFom = periode.getBeregningsgrunnlagPeriodeFom().plusMonths(2);
        BeregningsgrunnlagPeriode tredjePeriode = BeregningsgrunnlagPeriode.builder()
            .leggTilPeriodeÅrsak(PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV)
            .medBeregningsgrunnlagPeriode(tredjePeriodeFom, null).build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder(andelITredjePeriode)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdRef(ARBEIDSFORHOLD_REF).medRefusjonskravPrÅr(BigDecimal.TEN))
            .build(tredjePeriode);

        // Andre periode (opphør)
        BeregningsgrunnlagPrStatusOgAndel andelIAndrePeriode = Kopimaskin.deepCopy(arbeidstakerAndel);
        LocalDate andrePeriodeFom = periode.getBeregningsgrunnlagPeriodeFom().plusMonths(1);
        BeregningsgrunnlagPeriode andrePeriode = BeregningsgrunnlagPeriode.builder()
            .leggTilPeriodeÅrsak(PeriodeÅrsak.REFUSJON_OPPHØRER)
            .medBeregningsgrunnlagPeriode(andrePeriodeFom, tredjePeriodeFom.minusDays(1)).build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder(andelIAndrePeriode)
            .medBGAndelArbeidsforhold(arbeidsforholdBuilder.medRefusjonskravPrÅr(BigDecimal.ZERO))
            .build(andrePeriode);

        BeregningsgrunnlagPeriode.builder(periode).medBeregningsgrunnlagPeriode(periode.getBeregningsgrunnlagPeriodeFom(), andrePeriodeFom.minusDays(1));
        BeregningsgrunnlagPrStatusOgAndel.builder(arbeidstakerAndel)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdRef(ARBEIDSFORHOLD_REF).medRefusjonskravPrÅr(BigDecimal.ONE));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var input = new BeregningsgrunnlagInput(referanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(grunnlagEntitet);

        // Act
        List<FordelBeregningsgrunnlagArbeidsforholdDto> listeMedGraderingRefusjonDto = arbeidsforholdDtoTjeneste
            .lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(input);

        // Assert
        assertThat(listeMedGraderingRefusjonDto).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon()).hasSize(2);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErRefusjon()).isTrue();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErGradering()).isFalse();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getFom())
            .isEqualTo(periode.getBeregningsgrunnlagPeriodeFom());
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getTom())
            .isEqualTo(periode.getBeregningsgrunnlagPeriodeTom());

        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(1).isErRefusjon()).isTrue();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(1).isErGradering()).isFalse();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(1).getFom())
            .isEqualTo(tredjePeriode.getBeregningsgrunnlagPeriodeFom());
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(1).getTom()).isNull();
    }

    // 2 Arbeidsforhold for samme arbeidsgiver
    // Periode 1:
    // Arbeidsforhold 1 søker refusjon lik 10
    // Arbeidsforhold 2 søker ikke refusjon
    // Periode 2:
    // Arbeidsforhold 1 søker fortsatt refusjon lik 10
    // Arbeidsforhold 2 søker refusjon
    // Periode 3:
    // Arbeidsforhold 1 søker fortsatt refusjon lik 10
    // Arbeidsforhold 2 opphører refusjon
    @Test
    public void skal_lage_dto_med_refusjon_for_refusjon_med_fleire_perioder_uten_endring() {
        // Arrange
        InternArbeidsforholdRef arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();
        // Tredje periode (endring i refusjon)
        BeregningsgrunnlagPrStatusOgAndel andelITredjePeriode = Kopimaskin.deepCopy(arbeidstakerAndel);
        LocalDate tredjePeriodeFom = periode.getBeregningsgrunnlagPeriodeFom().plusMonths(2);
        BeregningsgrunnlagPeriode tredjePeriode = BeregningsgrunnlagPeriode.builder()
            .leggTilPeriodeÅrsak(PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV)
            .medBeregningsgrunnlagPeriode(tredjePeriodeFom, null).build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder(andelITredjePeriode)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdRef(ARBEIDSFORHOLD_REF).medRefusjonskravPrÅr(BigDecimal.TEN))
            .build(tredjePeriode);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdRef(arbeidsforholdId2).medRefusjonskravPrÅr(BigDecimal.ZERO))
            .build(tredjePeriode);

        // Andre periode (opphør)
        BeregningsgrunnlagPrStatusOgAndel andelIAndrePeriode = Kopimaskin.deepCopy(arbeidstakerAndel);
        LocalDate andrePeriodeFom = periode.getBeregningsgrunnlagPeriodeFom().plusMonths(1);
        BeregningsgrunnlagPeriode andrePeriode = BeregningsgrunnlagPeriode.builder()
            .leggTilPeriodeÅrsak(PeriodeÅrsak.REFUSJON_OPPHØRER)
            .medBeregningsgrunnlagPeriode(andrePeriodeFom, tredjePeriodeFom.minusDays(1)).build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder(andelIAndrePeriode)
            .medBGAndelArbeidsforhold(arbeidsforholdBuilder.medRefusjonskravPrÅr(BigDecimal.TEN))
            .build(andrePeriode);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdRef(arbeidsforholdId2).medRefusjonskravPrÅr(BigDecimal.TEN))
            .build(andrePeriode);

        BeregningsgrunnlagPeriode.builder(periode).medBeregningsgrunnlagPeriode(periode.getBeregningsgrunnlagPeriodeFom(), andrePeriodeFom.minusDays(1));
        BeregningsgrunnlagPrStatusOgAndel.builder(arbeidstakerAndel)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdRef(ARBEIDSFORHOLD_REF).medRefusjonskravPrÅr(BigDecimal.TEN));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdRef(arbeidsforholdId2).medRefusjonskravPrÅr(BigDecimal.ZERO))
            .build(periode);

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var input = new BeregningsgrunnlagInput(referanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(grunnlagEntitet);

        // Act
        List<FordelBeregningsgrunnlagArbeidsforholdDto> listeMedGraderingRefusjonDto = arbeidsforholdDtoTjeneste
            .lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(input);

        // Assert
        assertThat(listeMedGraderingRefusjonDto).hasSize(2);
        FordelBeregningsgrunnlagArbeidsforholdDto arbeidsforhold1 = listeMedGraderingRefusjonDto.stream()
            .filter(a -> a.getArbeidsforholdId().equals(ARBEIDSFORHOLD_REF.getReferanse())).findFirst().orElseThrow();
        FordelBeregningsgrunnlagArbeidsforholdDto arbeidsforhold2 = listeMedGraderingRefusjonDto.stream()
            .filter(a -> a.getArbeidsforholdId().equals(arbeidsforholdId2.getReferanse())).findFirst().orElseThrow();
        assertThat(arbeidsforhold1.getPerioderMedGraderingEllerRefusjon()).hasSize(1);
        assertThat(arbeidsforhold1.getPerioderMedGraderingEllerRefusjon().get(0).isErRefusjon()).isTrue();
        assertThat(arbeidsforhold1.getPerioderMedGraderingEllerRefusjon().get(0).isErGradering()).isFalse();
        assertThat(arbeidsforhold1.getPerioderMedGraderingEllerRefusjon().get(0).getFom()).isEqualTo(periode.getBeregningsgrunnlagPeriodeFom());
        assertThat(arbeidsforhold1.getPerioderMedGraderingEllerRefusjon().get(0).getTom()).isNull();

        assertThat(arbeidsforhold2.getPerioderMedGraderingEllerRefusjon()).hasSize(1);
        assertThat(arbeidsforhold2.getPerioderMedGraderingEllerRefusjon().get(0).isErRefusjon()).isTrue();
        assertThat(arbeidsforhold2.getPerioderMedGraderingEllerRefusjon().get(0).isErGradering()).isFalse();
        assertThat(arbeidsforhold2.getPerioderMedGraderingEllerRefusjon().get(0).getFom()).isEqualTo(andrePeriode.getBeregningsgrunnlagPeriodeFom());
        assertThat(arbeidsforhold2.getPerioderMedGraderingEllerRefusjon().get(0).getTom()).isEqualTo(andrePeriode.getBeregningsgrunnlagPeriodeTom());
    }

    // Periode 1:
    // Ingen søker refusjon
    // Periode 2:
    // Tilkommet arbeidsforhold søker refusjon
    @Test
    public void skal_lage_dto_med_refusjon_for_tilkommet_arbeidsforhold() {
        // Arrange
        var arbeidsforholdId2 = InternArbeidsforholdRef.nyRef();

        // Andre periode
        BeregningsgrunnlagPrStatusOgAndel andelIAndrePeriode = Kopimaskin.deepCopy(arbeidstakerAndel);
        LocalDate andrePeriodeFom = periode.getBeregningsgrunnlagPeriodeFom().plusMonths(1);
        BeregningsgrunnlagPeriode andrePeriode = BeregningsgrunnlagPeriode.builder()
            .leggTilPeriodeÅrsak(PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV)
            .medBeregningsgrunnlagPeriode(andrePeriodeFom, null).build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder(andelIAndrePeriode)
            .medBGAndelArbeidsforhold(arbeidsforholdBuilder.medRefusjonskravPrÅr(BigDecimal.ZERO))
            .build(andrePeriode);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdRef(arbeidsforholdId2).medRefusjonskravPrÅr(BigDecimal.TEN))
            .build(andrePeriode);

        // Første periode
        BeregningsgrunnlagPeriode.builder(periode).medBeregningsgrunnlagPeriode(periode.getBeregningsgrunnlagPeriodeFom(), andrePeriodeFom.minusDays(1));
        BeregningsgrunnlagPrStatusOgAndel.builder(arbeidstakerAndel)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdRef(ARBEIDSFORHOLD_REF).medRefusjonskravPrÅr(BigDecimal.ZERO));
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var input = new BeregningsgrunnlagInput(referanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(grunnlagEntitet);

        // Act
        List<FordelBeregningsgrunnlagArbeidsforholdDto> listeMedGraderingRefusjonDto = arbeidsforholdDtoTjeneste
            .lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(input);

        // Assert
        assertThat(listeMedGraderingRefusjonDto).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon()).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErRefusjon()).isTrue();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErGradering()).isFalse();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getFom())
            .isEqualTo(andrePeriode.getBeregningsgrunnlagPeriodeFom());
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getTom()).isNull();
    }

    @Test
    public void skal_lage_dto_med_gradering() {
        // Arrange
        LocalDate graderingTOM = SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2);
        DatoIntervallEntitet graderingPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_OPPTJENING, graderingTOM);
        AndelGradering andelGradering = AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiver1)
            .medArbeidsforholdRef(ARBEIDSFORHOLD_REF)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .leggTilGradering(new AndelGradering.Gradering(graderingPeriode, BigDecimal.TEN))
            .build();

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var input = new BeregningsgrunnlagInput(referanse, iayGrunnlag, null, new AktivitetGradering(andelGradering), null)
                .medBeregningsgrunnlagGrunnlag(grunnlagEntitet);

        // Act
        List<FordelBeregningsgrunnlagArbeidsforholdDto> listeMedGraderingRefusjonDto = arbeidsforholdDtoTjeneste
            .lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(input);

        // Assert
        assertThat(listeMedGraderingRefusjonDto).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon()).hasSize(1);
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErRefusjon()).isFalse();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).isErGradering()).isTrue();
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getFom())
            .isEqualTo(SKJÆRINGSTIDSPUNKT_OPPTJENING.toString());
        assertThat(listeMedGraderingRefusjonDto.get(0).getPerioderMedGraderingEllerRefusjon().get(0).getTom()).isEqualTo(graderingTOM.toString());
    }

    @Test
    public void skal_ikkje_lage_dto_for_andel_med_lagt_til_av_saksbehandler() {
        BeregningsgrunnlagPrStatusOgAndel.builder(arbeidstakerAndel).medLagtTilAvSaksbehandler(true);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
        var input = new BeregningsgrunnlagInput(referanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null)
                .medBeregningsgrunnlagGrunnlag(grunnlagEntitet);

        List<FordelBeregningsgrunnlagArbeidsforholdDto> listeMedGraderingRefusjonDto = arbeidsforholdDtoTjeneste
            .lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(input);
        assertThat(listeMedGraderingRefusjonDto).isEmpty();
    }

}
