package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.MatchBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsatteVerdierDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBeregningsgrunnlagAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.RedigerbarAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class FordelRefusjonTjenesteTest {

    public static final String ARBEIDSGIVER_ORGNR = "910909088";
    public static final int FORRIGE_ARBEIDSTINNTEKT = 100_000;
    public static final Inntektskategori FORRIGE_INNTEKTSKATEGORI = Inntektskategori.ARBEIDSTAKER;
    private final LocalDate FOM = LocalDate.now();
    private final LocalDate TOM = FOM.plusMonths(10);
    private final String ARB_ID1 = InternArbeidsforholdRef.nyRef().getReferanse();
    private final long ANDELSNR1 = 1L;
    private final long ANDELSNR2 = 2L;
    private final Integer FASTSATT = 10000;
    private final Integer REFUSJON = 10000;
    private final Integer REFUSJONPRÅR = REFUSJON *12;

    private final RedigerbarAndelDto ANDEL_UTEN_ARBEID_LAGT_TIL_OPPRETTET_INFO = new RedigerbarAndelDto(false, ANDELSNR1, true, AktivitetStatus.DAGPENGER, OpptjeningAktivitetType.DAGPENGER);
    private final RedigerbarAndelDto ANDEL_FRA_OPPRETTET_INFO = new RedigerbarAndelDto(false, ARBEIDSGIVER_ORGNR, ARB_ID1, ANDELSNR1, false, AktivitetStatus.ARBEIDSTAKER, OpptjeningAktivitetType.ARBEID);
    private final RedigerbarAndelDto ANDEL_FRA_OPPRETTET_UTEN_ARBEID_INFO = new RedigerbarAndelDto(false, ANDELSNR1, false, AktivitetStatus.ARBEIDSTAKER, OpptjeningAktivitetType.ARBEID);
    private final RedigerbarAndelDto ANDEL_LAGT_TIL_FORRIGE_INFO = new RedigerbarAndelDto(false, ARBEIDSGIVER_ORGNR, ARB_ID1, ANDELSNR2, true, AktivitetStatus.ARBEIDSTAKER, OpptjeningAktivitetType.ARBEID);
    private final RedigerbarAndelDto ANDEL_NY_INFO = new RedigerbarAndelDto(true, ARBEIDSGIVER_ORGNR, ARB_ID1, ANDELSNR1, true, AktivitetStatus.ARBEIDSTAKER, OpptjeningAktivitetType.ARBEID);

    private final FastsatteVerdierDto REFUSJON_NULL_FASTSATT_STØRRE_ENN_0 = new FastsatteVerdierDto(null, FASTSATT, Inntektskategori.ARBEIDSTAKER, null);
    private final FastsatteVerdierDto REFUSJON_STØRRE_ENN_0_FASTSATT_STØRRE_ENN_0 = new FastsatteVerdierDto(REFUSJON, FASTSATT, Inntektskategori.ARBEIDSTAKER, null);
    private final FastsatteVerdierDto REFUSJON_STØRRE_ENN_0_FASTSATT_HALVPARTEN = new FastsatteVerdierDto(REFUSJON, FASTSATT /2, Inntektskategori.ARBEIDSTAKER, null);
    private final FastsatteVerdierDto REFUSJON_NULL_FASTSATT_HALVPARTEN = new FastsatteVerdierDto(null, FASTSATT /2, Inntektskategori.ARBEIDSTAKER, null);
    private final FastsatteVerdierDto REFUSJON_STØRRE_ENN_0_FASTSATT_LIK_0 = new FastsatteVerdierDto(REFUSJON, 0, Inntektskategori.ARBEIDSTAKER, null);
    private final FastsatteVerdierDto REFUSJON_LIK_0_FASTSATT_LIK_0 = new FastsatteVerdierDto(0, 0, Inntektskategori.ARBEIDSTAKER, null);
    private final FastsatteVerdierDto REFUSJON_LIK_0_FASTSATT_STØRRE_ENN_0 = new FastsatteVerdierDto(0, FASTSATT, Inntektskategori.ARBEIDSTAKER, null);
    private final FastsatteVerdierDto REFUSJON_LIK_NULL_FASTSATT_LIK_0 = new FastsatteVerdierDto(null, 0, Inntektskategori.ARBEIDSTAKER, null);

    private BGAndelArbeidsforhold.Builder afBuilder1 = BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.virksomhet(ARBEIDSGIVER_ORGNR)).medArbeidsforholdRef(ARB_ID1).medRefusjonskravPrÅr(BigDecimal.ZERO);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
    private FordelRefusjonTjeneste fordelRefusjonTjeneste;
    private BeregningsgrunnlagPeriode periode;
    private FastsettBeregningsgrunnlagPeriodeDto endretPeriode;
    private List<FastsettBeregningsgrunnlagAndelDto> andelListe = new ArrayList<>();
    private BeregningsgrunnlagEntitet oppdatertBg;
    private BehandlingReferanse behandlingReferanse;
    private TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();

    @Before
    public void setUp() {
        this.behandlingReferanse = scenario.lagre(repositoryProvider);
        MatchBeregningsgrunnlagTjeneste matchBeregningsgrunnlagTjeneste = new MatchBeregningsgrunnlagTjeneste(repositoryProvider.getBeregningsgrunnlagRepository());
        this.fordelRefusjonTjeneste = new FordelRefusjonTjeneste(matchBeregningsgrunnlagTjeneste);
        oppdatertBg = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(BigDecimal.TEN)
            .medSkjæringstidspunkt(FOM).build();
        periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(FOM, TOM)
            .build(oppdatertBg);
        endretPeriode = new FastsettBeregningsgrunnlagPeriodeDto(andelListe, FOM, TOM);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), oppdatertBg, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    @Test
    public void skal_filtrere_ut_andel_uten_arbeidsforhold() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_UTEN_ARBEID_LAGT_TIL_OPPRETTET_INFO, REFUSJON_NULL_FASTSATT_STØRRE_ENN_0,
            Inntektskategori.DAGPENGER, null, 123_723);
        andelListe.add(fordeltAndel);
        lagDPAndelLagtTilAvSaksbehandler();

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        assertThat(map.get(fordeltAndel)).isNull();
    }


    @Test
    public void skal_sette_refusjon_lik_0_for_arbeidsforhold_uten_refusjonskrav() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_LIK_0_FASTSATT_STØRRE_ENN_0, FORRIGE_INNTEKTSKATEGORI, 0, FORRIGE_ARBEIDSTINNTEKT);
        andelListe.add(fordeltAndel);
        lagArbeidstakerAndel();

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        assertThat(map.get(fordeltAndel).intValue()).isEqualTo(0);
    }

    @Test
    public void skal_ikkje_fordele_refusjon_for_andeler_uten_arbeidsforhold() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_UTEN_ARBEID_INFO, REFUSJON_NULL_FASTSATT_STØRRE_ENN_0,
            Inntektskategori.ARBEIDSTAKER, 0, FORRIGE_ARBEIDSTINNTEKT);
        andelListe.add(fordeltAndel);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.BRUKERS_ANDEL)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBeregnetPrÅr(BigDecimal.valueOf(FORRIGE_ARBEIDSTINNTEKT))
            .medAndelsnr(1L)
            .build(periode);

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        assertThat(map.get(fordeltAndel)).isNull();
    }

    @Test
    public void skal_endre_refusjon_for_en_andel_med_refusjon_fastsatt_større_enn_0() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_STØRRE_ENN_0_FASTSATT_STØRRE_ENN_0, FORRIGE_INNTEKTSKATEGORI,
            REFUSJONPRÅR - 12, FORRIGE_ARBEIDSTINNTEKT);
        afBuilder1.medRefusjonskravPrÅr(BigDecimal.valueOf(REFUSJONPRÅR - 12));
        andelListe.add(fordeltAndel);
        lagArbeidstakerAndel();

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        assertThat(map.get(fordeltAndel).intValue()).isEqualTo(REFUSJONPRÅR);
    }

    @Test
    public void skal_endre_refusjon_for_en_andel_med_refusjon_fastsatt_lik_0() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_STØRRE_ENN_0_FASTSATT_LIK_0,
            Inntektskategori.ARBEIDSTAKER, REFUSJONPRÅR - 12, FORRIGE_ARBEIDSTINNTEKT);
        afBuilder1.medRefusjonskravPrÅr(BigDecimal.valueOf(REFUSJONPRÅR - 12));
        andelListe.add(fordeltAndel);
        lagArbeidstakerAndel();

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        assertThat(map.get(fordeltAndel).intValue()).isEqualTo(REFUSJONPRÅR);
    }

    @Test
    public void skal_ikkje_endre_refusjon_for_en_andel_uten_refusjon_fastsatt_lik_0() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_LIK_NULL_FASTSATT_LIK_0, FORRIGE_INNTEKTSKATEGORI, REFUSJONPRÅR - 12, FORRIGE_ARBEIDSTINNTEKT);
        afBuilder1.medRefusjonskravPrÅr(BigDecimal.valueOf(REFUSJONPRÅR - 12));
        andelListe.add(fordeltAndel);
        lagArbeidstakerAndel();

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        assertThat(map.get(fordeltAndel)).isNull();
    }

    @Test
    public void skal_sette_refusjon_lik_0_en_andel_med_refusjon_og_fastsatt_lik_0() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_LIK_0_FASTSATT_LIK_0, FORRIGE_INNTEKTSKATEGORI, REFUSJONPRÅR - 12, FORRIGE_ARBEIDSTINNTEKT);
        afBuilder1.medRefusjonskravPrÅr(BigDecimal.valueOf(REFUSJONPRÅR - 12));
        andelListe.add(fordeltAndel);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(afBuilder1)
            .build(periode);

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        assertThat(map.get(fordeltAndel).intValue()).isEqualTo(0);
    }

    @Test
    public void skal_fordele_refusjon_for_2_andeler_en_fra_opprettet_med_og_en_lagt_til_forrige_med_lik_refusjon_og_fordeling_større_enn_0() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_STØRRE_ENN_0_FASTSATT_STØRRE_ENN_0, FORRIGE_INNTEKTSKATEGORI, REFUSJONPRÅR - 12, FORRIGE_ARBEIDSTINNTEKT);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = new FastsettBeregningsgrunnlagAndelDto(ANDEL_LAGT_TIL_FORRIGE_INFO, REFUSJON_STØRRE_ENN_0_FASTSATT_STØRRE_ENN_0, null, null, null);
        andelListe.add(fordeltAndel);
        andelListe.add(fordeltAndel2);

        afBuilder1.medRefusjonskravPrÅr(BigDecimal.valueOf((REFUSJONPRÅR - 12)));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBeregnetPrÅr(BigDecimal.valueOf(FORRIGE_ARBEIDSTINNTEKT))
            .medAndelsnr(1L)
            .medBGAndelArbeidsforhold(afBuilder1)
            .build(periode);

        BGAndelArbeidsforhold.Builder afBuilder2 = lagArbeidsforholdMedRefusjonskrav(BigDecimal.valueOf((REFUSJONPRÅR - 12)));
        BeregningsgrunnlagEntitet forrigeBg = oppdatertBg.dypKopi();
        BeregningsgrunnlagPeriode periodeForrige = forrigeBg.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(afBuilder2)
            .medAndelsnr(2L)
            .build(periodeForrige);

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeBg, BeregningsgrunnlagTilstand.FASTSATT_INN);

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        assertThat(map.get(fordeltAndel).intValue()).isEqualTo(REFUSJONPRÅR);
        assertThat(map.get(fordeltAndel2).intValue()).isEqualTo(REFUSJONPRÅR);
    }

    @Test
    public void skal_fordele_refusjon_for_2_andeler_en_fra_opprettet_med_og_en_lagt_til_forrige_med_lik_refusjon_og_ulik_fordeling_større_enn_0() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_STØRRE_ENN_0_FASTSATT_STØRRE_ENN_0, FORRIGE_INNTEKTSKATEGORI, 54654, FORRIGE_ARBEIDSTINNTEKT);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = new FastsettBeregningsgrunnlagAndelDto(ANDEL_LAGT_TIL_FORRIGE_INFO, REFUSJON_STØRRE_ENN_0_FASTSATT_HALVPARTEN, null, null, null);
        andelListe.add(fordeltAndel);
        andelListe.add(fordeltAndel2);

        afBuilder1.medRefusjonskravPrÅr(BigDecimal.valueOf((54654)));
        lagArbeidstakerAndel();

        BGAndelArbeidsforhold.Builder afBuilder2 = lagArbeidsforholdMedRefusjonskrav(BigDecimal.valueOf((5465)));
        BeregningsgrunnlagEntitet forrigeBg = oppdatertBg.dypKopi();
        BeregningsgrunnlagPeriode periodeForrige = forrigeBg.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(afBuilder2)
            .medAndelsnr(2L)
            .build(periodeForrige);

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeBg, BeregningsgrunnlagTilstand.FASTSATT_INN);

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        double totalRefusjon = 2* REFUSJONPRÅR;
        int forventetRefusjon1 = (int) (2*totalRefusjon/3);
        int forventetRefusjon2 = (int) (totalRefusjon/3);
        assertThat(map.get(fordeltAndel2).intValue()).isEqualTo(forventetRefusjon2);
        assertThat(map.get(fordeltAndel).intValue()).isEqualTo(forventetRefusjon1);
    }

    @Test
    public void skal_fordele_refusjon_for_2_andeler_en_fra_opprettet_med_og_en_lagt_til_forrige_med_refusjon_lik_null_og_ulik_fordeling() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_NULL_FASTSATT_STØRRE_ENN_0, FORRIGE_INNTEKTSKATEGORI, 2* REFUSJONPRÅR, FORRIGE_ARBEIDSTINNTEKT);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = new FastsettBeregningsgrunnlagAndelDto(ANDEL_LAGT_TIL_FORRIGE_INFO, REFUSJON_NULL_FASTSATT_HALVPARTEN, null, null, null);
        andelListe.add(fordeltAndel);
        andelListe.add(fordeltAndel2);

        afBuilder1.medRefusjonskravPrÅr(BigDecimal.valueOf((2* REFUSJONPRÅR)));
        lagArbeidstakerAndel();

        BGAndelArbeidsforhold.Builder afBuilder2 = lagArbeidsforholdMedRefusjonskrav();
        BGAndelArbeidsforhold.Builder afBuilder3 = lagArbeidsforholdMedRefusjonskrav();
        BeregningsgrunnlagEntitet forrigeBg = oppdatertBg.dypKopi();
        BeregningsgrunnlagPeriode periodeForrige = forrigeBg.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(periodeForrige.getBeregningsgrunnlagPrStatusOgAndelList().get(0))
            .medBGAndelArbeidsforhold(afBuilder3);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(afBuilder2)
            .medAndelsnr(2L)
            .build(periodeForrige);

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeBg, BeregningsgrunnlagTilstand.FASTSATT_INN);

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        double totalRefusjon = 2* REFUSJONPRÅR;
        int forventetRefusjon1 = (int) (2*totalRefusjon/3);
        int forventetRefusjon2 = (int) (totalRefusjon/3);
        assertThat(map.get(fordeltAndel2).intValue()).isEqualTo(forventetRefusjon2);
        assertThat(map.get(fordeltAndel).intValue()).isEqualTo(forventetRefusjon1);
    }

    @Test
    public void skal_fordele_refusjon_for_3_andeler_en_fra_opprettet_med_og_en_lagt_til_forrige_en_ny_andel_lik_fordeling_ulik_0() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_STØRRE_ENN_0_FASTSATT_STØRRE_ENN_0, FORRIGE_INNTEKTSKATEGORI, 54654, FORRIGE_ARBEIDSTINNTEKT);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = new FastsettBeregningsgrunnlagAndelDto(ANDEL_LAGT_TIL_FORRIGE_INFO, REFUSJON_STØRRE_ENN_0_FASTSATT_STØRRE_ENN_0, null, null, null);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel3 = new FastsettBeregningsgrunnlagAndelDto(ANDEL_NY_INFO, REFUSJON_NULL_FASTSATT_STØRRE_ENN_0, null, null, null);
        andelListe.add(fordeltAndel);
        andelListe.add(fordeltAndel2);
        andelListe.add(fordeltAndel3);

        afBuilder1.medRefusjonskravPrÅr(BigDecimal.valueOf((54654)));
        lagArbeidstakerAndel();

        BGAndelArbeidsforhold.Builder afBuilder2 = lagArbeidsforholdMedRefusjonskrav(BigDecimal.valueOf((5465)));
        BeregningsgrunnlagEntitet forrigeBg = oppdatertBg.dypKopi();
        BeregningsgrunnlagPeriode periodeForrige = forrigeBg.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(afBuilder2)
            .medAndelsnr(2L)
            .build(periodeForrige);

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeBg, BeregningsgrunnlagTilstand.FASTSATT_INN);

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        double totalRefusjon = 2* REFUSJONPRÅR;
        int forventetRefusjon = (int) (totalRefusjon/3);
        assertThat(map.get(fordeltAndel).intValue()).isEqualTo(forventetRefusjon);
        assertThat(map.get(fordeltAndel2).intValue()).isEqualTo(forventetRefusjon);
        assertThat(map.get(fordeltAndel3).intValue()).isEqualTo(forventetRefusjon);
    }

    @Test
    public void skal_fordele_refusjon_for_3_andeler_en_fra_opprettet_med_og_en_lagt_til_forrige_en_ny_andel_ulik_fordeling() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_STØRRE_ENN_0_FASTSATT_STØRRE_ENN_0, FORRIGE_INNTEKTSKATEGORI, 54654, FORRIGE_ARBEIDSTINNTEKT);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = new FastsettBeregningsgrunnlagAndelDto(ANDEL_LAGT_TIL_FORRIGE_INFO, REFUSJON_STØRRE_ENN_0_FASTSATT_HALVPARTEN,null, null, null);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel3 = new FastsettBeregningsgrunnlagAndelDto(ANDEL_NY_INFO, REFUSJON_NULL_FASTSATT_HALVPARTEN, null, null, null);
        andelListe.add(fordeltAndel);
        andelListe.add(fordeltAndel2);
        andelListe.add(fordeltAndel3);

        afBuilder1.medRefusjonskravPrÅr(BigDecimal.valueOf((54654)));
        lagArbeidstakerAndel();

        BGAndelArbeidsforhold.Builder afBuilder2 = lagArbeidsforholdMedRefusjonskrav(BigDecimal.valueOf((5465)));
        BeregningsgrunnlagEntitet forrigeBg = oppdatertBg.dypKopi();
        BeregningsgrunnlagPeriode periodeForrige = forrigeBg.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(afBuilder2)
            .medAndelsnr(2L)
            .build(periodeForrige);

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeBg, BeregningsgrunnlagTilstand.FASTSATT_INN);

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        double totalRefusjon = 2* REFUSJONPRÅR;
        int forventetRefusjon1 = (int) (2*totalRefusjon/4);
        int forventetRefusjon2 = (int) (totalRefusjon/4);
        int forventetRefusjon3 = (int) (totalRefusjon/4);
        assertThat(map.get(fordeltAndel).intValue()).isEqualTo(forventetRefusjon1);
        assertThat(map.get(fordeltAndel2).intValue()).isEqualTo(forventetRefusjon2);
        assertThat(map.get(fordeltAndel3).intValue()).isEqualTo(forventetRefusjon3);
    }

    @Test
    public void skal_fordele_refusjon_for_3_andeler_en_fra_opprettet_med_og_en_lagt_til_forrige_en_ny_andel_ulik_fordeling_refusjon_lik_null() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_NULL_FASTSATT_STØRRE_ENN_0, FORRIGE_INNTEKTSKATEGORI, 2* REFUSJONPRÅR, FORRIGE_ARBEIDSTINNTEKT);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = new FastsettBeregningsgrunnlagAndelDto(ANDEL_LAGT_TIL_FORRIGE_INFO, REFUSJON_NULL_FASTSATT_HALVPARTEN, null, null, null);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel3 = new FastsettBeregningsgrunnlagAndelDto(ANDEL_NY_INFO, REFUSJON_NULL_FASTSATT_HALVPARTEN, null, null, null);
        andelListe.add(fordeltAndel);
        andelListe.add(fordeltAndel2);
        andelListe.add(fordeltAndel3);

        afBuilder1.medRefusjonskravPrÅr(BigDecimal.valueOf((2* REFUSJONPRÅR)));
        lagArbeidstakerAndel();

        BGAndelArbeidsforhold.Builder afBuilder2 = lagArbeidsforholdMedRefusjonskrav();
        BGAndelArbeidsforhold.Builder afBuilder3 = lagArbeidsforholdMedRefusjonskrav();
        BeregningsgrunnlagEntitet forrigeBg = oppdatertBg.dypKopi();
        BeregningsgrunnlagPeriode periodeForrige = forrigeBg.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(periodeForrige.getBeregningsgrunnlagPrStatusOgAndelList().get(0))
            .medBGAndelArbeidsforhold(afBuilder3);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(afBuilder2)
            .medAndelsnr(2L)
            .build(periodeForrige);

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeBg, BeregningsgrunnlagTilstand.FASTSATT_INN);

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        double totalRefusjon = 2* REFUSJONPRÅR;
        int forventetRefusjon1 = (int) (2*totalRefusjon/4);
        int forventetRefusjon2 = (int) (totalRefusjon/4);
        int forventetRefusjon3 = (int) (totalRefusjon/4);
        assertThat(map.get(fordeltAndel).intValue()).isEqualTo(forventetRefusjon1);
        assertThat(map.get(fordeltAndel2).intValue()).isEqualTo(forventetRefusjon2);
        assertThat(map.get(fordeltAndel3).intValue()).isEqualTo(forventetRefusjon3);
    }

    @Test
    public void skal_fordele_refusjon_for_3_andeler_en_fra_opprettet_med_og_en_lagt_til_forrige_en_ny_andel_lik_fordeling_refusjon_lik_null() {
        // Arrange
        FastsettBeregningsgrunnlagAndelDto fordeltAndel = new FastsettBeregningsgrunnlagAndelDto(ANDEL_FRA_OPPRETTET_INFO, REFUSJON_NULL_FASTSATT_STØRRE_ENN_0, FORRIGE_INNTEKTSKATEGORI,  2* REFUSJONPRÅR, FORRIGE_ARBEIDSTINNTEKT);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel2 = new FastsettBeregningsgrunnlagAndelDto(ANDEL_LAGT_TIL_FORRIGE_INFO, REFUSJON_NULL_FASTSATT_STØRRE_ENN_0, null, null, null);
        FastsettBeregningsgrunnlagAndelDto fordeltAndel3 = new FastsettBeregningsgrunnlagAndelDto(ANDEL_NY_INFO, REFUSJON_NULL_FASTSATT_STØRRE_ENN_0, null, null, null);
        andelListe.add(fordeltAndel);
        andelListe.add(fordeltAndel2);
        andelListe.add(fordeltAndel3);

        afBuilder1.medRefusjonskravPrÅr(BigDecimal.valueOf((2* REFUSJONPRÅR)));
        lagArbeidstakerAndel();

        BGAndelArbeidsforhold.Builder afBuilder2 = lagArbeidsforholdMedRefusjonskrav();
        BGAndelArbeidsforhold.Builder afBuilder3 = lagArbeidsforholdMedRefusjonskrav();
        BeregningsgrunnlagEntitet forrigeBg = oppdatertBg.dypKopi();
        BeregningsgrunnlagPeriode periodeForrige = forrigeBg.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(periodeForrige.getBeregningsgrunnlagPrStatusOgAndelList().get(0))
            .medBGAndelArbeidsforhold(afBuilder3);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(afBuilder2)
            .medAndelsnr(2L)
            .build(periodeForrige);

        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), forrigeBg, BeregningsgrunnlagTilstand.FASTSATT_INN);

        // Act
        Map<FastsettBeregningsgrunnlagAndelDto, BigDecimal> map = fordelRefusjonTjeneste.getRefusjonPrÅrMap(behandlingReferanse.getId(), endretPeriode, periode);

        // Assert
        double totalRefusjon = 2* REFUSJONPRÅR;
        int forventetRefusjon = (int) (totalRefusjon/3);
        assertThat(map.get(fordeltAndel).intValue()).isEqualTo(forventetRefusjon);
        assertThat(map.get(fordeltAndel2).intValue()).isEqualTo(forventetRefusjon);
        assertThat(map.get(fordeltAndel3).intValue()).isEqualTo(forventetRefusjon);
    }

    private BGAndelArbeidsforhold.Builder lagArbeidsforholdMedRefusjonskrav() {
        return BGAndelArbeidsforhold.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ARBEIDSGIVER_ORGNR))
            .medArbeidsforholdRef(ARB_ID1)
            .medRefusjonskravPrÅr(BigDecimal.valueOf((REFUSJONPRÅR)));
    }

    private BGAndelArbeidsforhold.Builder lagArbeidsforholdMedRefusjonskrav(BigDecimal refusjonskrav) {
        return BGAndelArbeidsforhold.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ARBEIDSGIVER_ORGNR))
            .medArbeidsforholdRef(ARB_ID1)
            .medRefusjonskravPrÅr(refusjonskrav);
    }

    private void lagArbeidstakerAndel() {
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(FORRIGE_INNTEKTSKATEGORI)
            .medBGAndelArbeidsforhold(afBuilder1)
            .medBeregnetPrÅr(BigDecimal.valueOf(FORRIGE_ARBEIDSTINNTEKT))
            .medAndelsnr(1L)
            .build(periode);
    }

    private void lagDPAndelLagtTilAvSaksbehandler() {
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.DAGPENGER)
            .medInntektskategori(Inntektskategori.DAGPENGER)
            .medAndelsnr(1L)
            .medLagtTilAvSaksbehandler(true)
            .build(periode);
    }
}
