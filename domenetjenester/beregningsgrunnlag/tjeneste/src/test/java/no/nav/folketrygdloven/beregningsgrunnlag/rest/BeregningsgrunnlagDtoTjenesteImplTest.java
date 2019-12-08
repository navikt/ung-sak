package no.nav.folketrygdloven.beregningsgrunnlag.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Hjemmel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.SammenligningsgrunnlagPrStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.SammenligningsgrunnlagType;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.SammenligningsgrunnlagDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.AndelerForFaktaOmBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.AvklarAktiviteterDtoTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.BeregningsgrunnlagDtoTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.BeregningsgrunnlagDtoUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.BeregningsgrunnlagPrStatusOgAndelDtoTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.FaktaOmBeregningDtoTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.FaktaOmBeregningTilfelleDtoTjenesteProviderMock;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.FaktaOmFordelingDtoTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.VisningsnavnForAktivitetTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjenesteImpl;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.felles.integrasjon.organisasjon.OrganisasjonConsumer;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;

public class BeregningsgrunnlagDtoTjenesteImplTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(5);
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(repositoryRule.getEntityManager());
    private VirksomhetTjeneste virksomhetTjeneste = Mockito.spy(new VirksomhetTjeneste(Mockito.mock(OrganisasjonConsumer.class), repositoryProvider.getVirksomhetRepository()));
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    @Mock
    private PersonIdentTjeneste tpsTjenesteMock;

    private BehandlingReferanse behandlingReferanse;
    private VirksomhetEntitet virksomhet;
    private BeregningsgrunnlagDtoTjeneste beregningsgrunnlagDtoTjeneste;

    private BigDecimal grunnbeløp;

    private static final Inntektskategori INNTEKTSKATEGORI = Inntektskategori.ARBEIDSTAKER;
    private static final BigDecimal AVKORTET_PR_AAR = BigDecimal.valueOf(150000);
    private static final BigDecimal BRUTTO_PR_AAR = BigDecimal.valueOf(300000);
    private static final BigDecimal REDUSERT_PR_AAR = BigDecimal.valueOf(500000);
    private static final BigDecimal OVERSTYRT_PR_AAR = BigDecimal.valueOf(500);
    private static final BigDecimal PGI_SNITT = BigDecimal.valueOf(400000);
    private static final LocalDate ANDEL_FOM = LocalDate.now().minusDays(100);
    private static final LocalDate ANDEL_TOM = LocalDate.now();
    private static final String ORGNR = "973093681";
    private static final Long ANDELSNR = 1L;
    private static final String PRIVATPERSON_NAVN = "Skrue McDuck";
    private static final String PRIVATPERSON_IDENT = "9988776655443";
    private static final LocalDate FØDSELSDATO = LocalDate.of(2000, 1, 1);

    // sammenligningsgrunnlag
    private static final BigDecimal RAPPORTERT_PR_AAR = BigDecimal.valueOf(300000);
    private static final long AVVIK_OVER_25_PROSENT = 500L;
    private static final long AVVIK_UNDER_25_PROSENT = 30L;
    private static final LocalDate SAMMENLIGNING_FOM = LocalDate.now().minusDays(100);
    private static final LocalDate SAMMENLIGNING_TOM = LocalDate.now();
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(repositoryRule.getEntityManager());
    private BeregningsgrunnlagGrunnlagEntitet grunnlag;
    private OpptjeningAktiviteter opptjeningAktiviteter;
    private BeregningAktivitetAggregatEntitet beregningAktiviteter;

    @Before
    public void setup() {
        initMocks(this);
        grunnbeløp = BigDecimal.valueOf(beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, SKJÆRINGSTIDSPUNKT).getVerdi());
        when(tpsTjenesteMock.hentBrukerForAktør(Mockito.any(AktørId.class))).thenReturn(Optional.of(lagPersoninfo()));
        virksomhet = new VirksomhetEntitet.Builder()
            .medOrgnr(ORGNR)
            .medNavn("VirksomhetNavn")
            .oppdatertOpplysningerNå()
            .build();
        repositoryProvider.getVirksomhetRepository().lagre(virksomhet);
        ArbeidsgiverTjeneste arbeidsgiverTjeneste = new ArbeidsgiverTjenesteImpl(tpsTjenesteMock, virksomhetTjeneste);
        BeregningsgrunnlagDtoUtil beregningsgrunnlagDtoUtil = new BeregningsgrunnlagDtoUtil(beregningsgrunnlagRepository, arbeidsgiverTjeneste);
        AvklarAktiviteterDtoTjeneste avklarAktiviteterDtoTjeneste = new AvklarAktiviteterDtoTjeneste(arbeidsgiverTjeneste);
        VisningsnavnForAktivitetTjeneste visningsnavnForAktivitetTjeneste = new VisningsnavnForAktivitetTjeneste(arbeidsgiverTjeneste);
        FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste = new FordelBeregningsgrunnlagTjeneste();
        AndelerForFaktaOmBeregningTjeneste andelerForFaktaOmBeregningTjeneste = new AndelerForFaktaOmBeregningTjeneste(beregningsgrunnlagRepository,
            visningsnavnForAktivitetTjeneste,
            beregningsgrunnlagDtoUtil, refusjonOgGraderingTjeneste);
        FaktaOmBeregningDtoTjeneste faktaOmBeregningDtoTjeneste = new FaktaOmBeregningDtoTjeneste(
            beregningsgrunnlagRepository,
            FaktaOmBeregningTilfelleDtoTjenesteProviderMock.getTjenesteInstances(repositoryProvider),
            avklarAktiviteterDtoTjeneste,
            andelerForFaktaOmBeregningTjeneste
        );
        FaktaOmFordelingDtoTjeneste faktaOmFordelingDtoTjeneste = mock(FaktaOmFordelingDtoTjeneste.class);
        when(faktaOmFordelingDtoTjeneste.lagDto(any())).thenReturn(Optional.empty());
        BeregningsgrunnlagPrStatusOgAndelDtoTjeneste andelDtoTjeneste = new BeregningsgrunnlagPrStatusOgAndelDtoTjeneste(
            beregningsgrunnlagDtoUtil,
            refusjonOgGraderingTjeneste
        );
        beregningsgrunnlagDtoTjeneste = new BeregningsgrunnlagDtoTjeneste(beregningsgrunnlagRepository,
            faktaOmBeregningDtoTjeneste,
            andelDtoTjeneste);
        Mockito.doReturn(virksomhet).when(virksomhetTjeneste).hentOgLagreOrganisasjon(any(String.class));
    }

    @Test
    public void skal_teste_at_beregningsgrunnlagDto_aktivitetStatus_får_korrekte_verdier() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        lagBehandlingMedBgOgOpprettFagsakRelasjon(arbeidsgiver);
        // Act
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), List.of(), grunnlag);

        // Assert
        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(grunnlagDto -> {
            List<AktivitetStatus> aktivitetStatus = grunnlagDto.getAktivitetStatus();
            assertThat(aktivitetStatus).isNotNull();
            assertThat(aktivitetStatus.get(0)).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        });
    }

    @Test
    public void skal_teste_at_beregningsgrunnlagDto_sammenligningsgrunnlag_får_korrekte_verdier() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        lagBehandlingMedBgOgOpprettFagsakRelasjon(arbeidsgiver);

        // Act
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), List.of(), grunnlag);

        // Assert
        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto -> {
            SammenligningsgrunnlagDto sammenligningsgrunnlag = beregningsgrunnlagDto.getSammenligningsgrunnlag();
            assertThat(sammenligningsgrunnlag).isNotNull();
            assertThat(sammenligningsgrunnlag.getAvvikPromille()).isEqualTo(AVVIK_OVER_25_PROSENT);
            assertThat(sammenligningsgrunnlag.getRapportertPrAar()).isEqualTo(RAPPORTERT_PR_AAR);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagFom()).isEqualTo(SAMMENLIGNING_FOM);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagTom()).isEqualTo(SAMMENLIGNING_TOM);
        });

        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto -> {
            SammenligningsgrunnlagDto sammenligningsgrunnlag = beregningsgrunnlagDto.getSammenligningsgrunnlagPrStatus().get(0);
            assertThat(sammenligningsgrunnlag).isNotNull();
            assertThat(sammenligningsgrunnlag.getAvvikPromille()).isEqualTo(AVVIK_OVER_25_PROSENT);
            assertThat(sammenligningsgrunnlag.getRapportertPrAar()).isEqualTo(RAPPORTERT_PR_AAR);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagFom()).isEqualTo(SAMMENLIGNING_FOM);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagTom()).isEqualTo(SAMMENLIGNING_TOM);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagType()).isEqualTo(SammenligningsgrunnlagType.SAMMENLIGNING_ATFL_SN);
            assertThat(sammenligningsgrunnlag.getDifferanseBeregnet()).isEqualTo(OVERSTYRT_PR_AAR.subtract(RAPPORTERT_PR_AAR));
        });
    }

    @Test
    public void skal_teste_at_beregningsgrunnlagDto_lages() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        lagBehandlingMedBgOgOpprettFagsakRelasjon(arbeidsgiver);
        // Act
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), List.of(), grunnlag);

        // Assert
        assertThat(beregningsgrunnlagDtoOpt).isPresent();
    }

    @Test
    public void skal_teste_at_beregningsgrunnlagDto_får_korrekte_verdier() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        lagBehandlingMedBgOgOpprettFagsakRelasjon(arbeidsgiver);

        // Act
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), List.of(), grunnlag);

        // Assert
        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(grunnlagDto -> {
            assertThat(grunnlagDto).isNotNull();
            assertThat(grunnlagDto.getSkjaeringstidspunktBeregning()).isEqualTo(SKJÆRINGSTIDSPUNKT);
            assertThat(grunnlagDto.getLedetekstAvkortet()).isNotNull();
            assertThat(grunnlagDto.getLedetekstBrutto()).isNotNull();
            assertThat(grunnlagDto.getHalvG().intValue()).isEqualTo(grunnbeløp.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP).intValue());
        });
    }

    @Test
    public void skal_teste_at_beregningsgrunnlagDto_får_korrekte_verdier_om_fakta_om_beregning_er_utført_uten_fastsatt_inntekt() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        BeregningsgrunnlagEntitet faktaOmBeregningBg = lagBehandlingMedBgOgOpprettFagsakRelasjon(arbeidsgiver);
        BeregningsgrunnlagEntitet fastsattGrunnlag = faktaOmBeregningBg.dypKopi();
        BeregningsgrunnlagPrStatusOgAndel andel = fastsattGrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        BigDecimal beregnetEtterFastsattSteg = BigDecimal.valueOf(10000);
        BeregningsgrunnlagPrStatusOgAndel.builder(andel).medBeregnetPrÅr(beregnetEtterFastsattSteg);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), fastsattGrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT_UT);

        // Act
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), List.of(), grunnlag);

        // Assert
        assertBeregningsgrunnlag(beregnetEtterFastsattSteg, beregningsgrunnlagDtoOpt);
    }

    @Test
    public void skal_teste_at_beregningsgrunnlagDto_får_korrekte_verdier_om_fakta_om_beregning_er_utført_med_fastsatt_inntekt() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        BeregningsgrunnlagEntitet opprettetGrunnlag = lagBehandlingMedBgOgOpprettFagsakRelasjon(arbeidsgiver);

        // Lag kofakber grunnlag
        BeregningsgrunnlagEntitet faktaOmBeregningGrunnlag = opprettetGrunnlag.dypKopi();
        BeregningsgrunnlagPrStatusOgAndel andel = faktaOmBeregningGrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        BigDecimal fastsattIKofakBer = BigDecimal.valueOf(10000);
        BeregningsgrunnlagPrStatusOgAndel.builder(andel)
            .medMottarYtelse(true, AktivitetStatus.ARBEIDSTAKER)
            .medFastsattAvSaksbehandler(true)
            .medBeregnetPrÅr(fastsattIKofakBer);
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(), faktaOmBeregningGrunnlag, BeregningsgrunnlagTilstand.KOFAKBER_UT);

        // Gjenopprett opprettet grunnlag
        beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPRETTET);

        // Act
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), List.of(), grunnlag);

        // Assert
        assertBeregningsgrunnlag(fastsattIKofakBer, beregningsgrunnlagDtoOpt);
    }

    @Test
    public void skal_teste_at_beregningsgrunnlagDto_beregningsgrunnlagperiode_får_korrekte_verdier() {
        lagBehandlingMedBgOgOpprettFagsakRelasjon(Arbeidsgiver.virksomhet(ORGNR));
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), List.of(), grunnlag);

        // Assert
        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto -> {
            List<BeregningsgrunnlagPeriodeDto> beregningsgrunnlagPeriodeDtoList = beregningsgrunnlagDto.getBeregningsgrunnlagPeriode();
            assertThat(beregningsgrunnlagPeriodeDtoList).hasSize(1);

            BeregningsgrunnlagPeriodeDto periodeDto = beregningsgrunnlagPeriodeDtoList.get(0);
            List<BeregningsgrunnlagPrStatusOgAndelDto> andelList = periodeDto.getBeregningsgrunnlagPrStatusOgAndel();
            assertThat(andelList).hasSize(1);
            BeregningsgrunnlagPrStatusOgAndelDto andelDto = andelList.get(0);
            assertThat(andelDto.getInntektskategori()).isEqualByComparingTo(INNTEKTSKATEGORI);
            assertThat(andelDto.getAndelsnr()).isEqualTo(ANDELSNR);
            assertThat(andelDto.getAvkortetPrAar()).isEqualTo(AVKORTET_PR_AAR);
            assertThat(andelDto.getRedusertPrAar()).isEqualTo(REDUSERT_PR_AAR);
            assertThat(andelDto.getBruttoPrAar()).isEqualTo(OVERSTYRT_PR_AAR);
            assertThat(andelDto.getBeregnetPrAar()).isEqualTo(BRUTTO_PR_AAR);
            assertThat(andelDto.getBeregningsgrunnlagFom()).isEqualTo(ANDEL_FOM);
            assertThat(andelDto.getBeregningsgrunnlagTom()).isEqualTo(ANDEL_TOM);
            assertThat(andelDto.getArbeidsforhold()).isNotNull();
            assertThat(andelDto.getArbeidsforhold().getArbeidsgiverNavn()).isEqualTo(virksomhet.getNavn());
            assertThat(andelDto.getArbeidsforhold().getArbeidsgiverId()).isEqualTo(virksomhet.getOrgnr());
        });
    }

    @Test
    public void skal_teste_at_beregningsgrunnlagDto_beregningsgrunnlagperiode_får_korrekte_verdier_ved_arbeidsgiver_privatperson() {
        // Arrange
        lagBehandlingMedBgOgOpprettFagsakRelasjon(Arbeidsgiver.person(AktørId.dummy()));
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), List.of(), grunnlag);

        // Assert
        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto -> {
            List<BeregningsgrunnlagPeriodeDto> beregningsgrunnlagPeriodeDtoList = beregningsgrunnlagDto.getBeregningsgrunnlagPeriode();
            assertThat(beregningsgrunnlagPeriodeDtoList).hasSize(1);

            BeregningsgrunnlagPeriodeDto periodeDto = beregningsgrunnlagPeriodeDtoList.get(0);
            List<BeregningsgrunnlagPrStatusOgAndelDto> andelList = periodeDto.getBeregningsgrunnlagPrStatusOgAndel();
            assertThat(andelList).hasSize(1);
            BeregningsgrunnlagPrStatusOgAndelDto andelDto = andelList.get(0);
            assertThat(andelDto.getInntektskategori()).isEqualByComparingTo(INNTEKTSKATEGORI);
            assertThat(andelDto.getAndelsnr()).isEqualTo(ANDELSNR);
            assertThat(andelDto.getAvkortetPrAar()).isEqualTo(AVKORTET_PR_AAR);
            assertThat(andelDto.getRedusertPrAar()).isEqualTo(REDUSERT_PR_AAR);
            assertThat(andelDto.getBruttoPrAar()).isEqualTo(OVERSTYRT_PR_AAR);
            assertThat(andelDto.getBeregnetPrAar()).isEqualTo(BRUTTO_PR_AAR);
            assertThat(andelDto.getBeregningsgrunnlagFom()).isEqualTo(ANDEL_FOM);
            assertThat(andelDto.getBeregningsgrunnlagTom()).isEqualTo(ANDEL_TOM);
            assertThat(andelDto.getArbeidsforhold()).isNotNull();
            assertThat(andelDto.getArbeidsforhold().getArbeidsgiverNavn()).isEqualTo(PRIVATPERSON_NAVN);
            assertThat(andelDto.getArbeidsforhold().getArbeidsgiverId()).isEqualTo("01.01.2000");
        });
    }

    // Tester at verdier avklart i tidligere fastsatt-steg sendes til frontend (etter bekreftet faka om beregning)
    @Test
    public void skal_teste_at_beregningsgrunnlagDto_beregningsgrunnlagperiode_får_korrekte_verdier_ved_løst_aksjonspunkt_tidligere_i_steg_100() {
        lagBehandlingMedBgOgOpprettFagsakRelasjon(Arbeidsgiver.virksomhet(ORGNR));
        int inntektSattIFaktaOmBeregning = 10000;
        Inntektskategori inntektskategoriSattIFaktaOmBeregning = Inntektskategori.SJØMANN;
        lagBgMedRedigerteVerdierIFaktaOmBeregning(inntektSattIFaktaOmBeregning, inntektskategoriSattIFaktaOmBeregning);
        int overstyrtForrigeGang = 45000;
        lagBgMedOverstyrteVerdierISteg100(overstyrtForrigeGang);
        lagOpprettetBg();
        int nyInntektIFaktaOmBeregning = 32322;
        lagBgMedRedigerteVerdierIFaktaOmBeregning(nyInntektIFaktaOmBeregning, inntektskategoriSattIFaktaOmBeregning);
        List<Inntektsmelding> inntektsmeldinger = List.of();
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), inntektsmeldinger, grunnlag);

        // Assert
        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto ->
            assertFastsattVerdier(inntektskategoriSattIFaktaOmBeregning, overstyrtForrigeGang, nyInntektIFaktaOmBeregning, beregningsgrunnlagDto)
        );
    }

    @Test
    public void skalSetteSammenligningsgrunnlagDtoMedDifferanseNårFlereAndeler() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        lagBehandlingMedBgOgOpprettFagsakRelasjonFlereAndeler(arbeidsgiver);
        // Act
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), List.of(), grunnlag);
        // Assert
        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto -> {
            SammenligningsgrunnlagDto sammenligningsgrunnlag = beregningsgrunnlagDto.getSammenligningsgrunnlagPrStatus().get(0);
            assertThat(sammenligningsgrunnlag).isNotNull();
            assertThat(sammenligningsgrunnlag.getAvvikPromille()).isEqualTo(AVVIK_OVER_25_PROSENT);
            assertThat(sammenligningsgrunnlag.getRapportertPrAar()).isEqualTo(RAPPORTERT_PR_AAR);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagFom()).isEqualTo(SAMMENLIGNING_FOM);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagTom()).isEqualTo(SAMMENLIGNING_TOM);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagType()).isEqualTo(SammenligningsgrunnlagType.SAMMENLIGNING_AT);
            assertThat(sammenligningsgrunnlag.getDifferanseBeregnet()).isEqualTo(OVERSTYRT_PR_AAR.subtract(RAPPORTERT_PR_AAR));
        });

        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto -> {
            SammenligningsgrunnlagDto sammenligningsgrunnlag = beregningsgrunnlagDto.getSammenligningsgrunnlagPrStatus().get(1);
            assertThat(sammenligningsgrunnlag).isNotNull();
            assertThat(sammenligningsgrunnlag.getAvvikPromille()).isEqualTo(AVVIK_UNDER_25_PROSENT);
            assertThat(sammenligningsgrunnlag.getRapportertPrAar()).isEqualTo(RAPPORTERT_PR_AAR);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagFom()).isEqualTo(SAMMENLIGNING_FOM);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagTom()).isEqualTo(SAMMENLIGNING_TOM);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagType()).isEqualTo(SammenligningsgrunnlagType.SAMMENLIGNING_FL);
            assertThat(sammenligningsgrunnlag.getDifferanseBeregnet()).isEqualTo(BRUTTO_PR_AAR.subtract(RAPPORTERT_PR_AAR));
        });

        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto -> {
            SammenligningsgrunnlagDto sammenligningsgrunnlag = beregningsgrunnlagDto.getSammenligningsgrunnlagPrStatus().get(2);
            assertThat(sammenligningsgrunnlag).isNotNull();
            assertThat(sammenligningsgrunnlag.getAvvikPromille()).isEqualTo(AVVIK_OVER_25_PROSENT);
            assertThat(sammenligningsgrunnlag.getRapportertPrAar()).isEqualTo(RAPPORTERT_PR_AAR);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagFom()).isEqualTo(SAMMENLIGNING_FOM);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagTom()).isEqualTo(SAMMENLIGNING_TOM);
            assertThat(sammenligningsgrunnlag.getSammenligningsgrunnlagType()).isEqualTo(SammenligningsgrunnlagType.SAMMENLIGNING_SN);
            assertThat(sammenligningsgrunnlag.getDifferanseBeregnet()).isEqualTo(PGI_SNITT.subtract(RAPPORTERT_PR_AAR));
        });
    }

    @Test
    public void skalSetteFastsettingGrunnlagForHverBeregningsgrunnlagPrStatusOgAndelNårFlereAndelerMedUlikeAvvik() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        lagBehandlingMedBgOgOpprettFagsakRelasjonFlereAndeler(arbeidsgiver);
        // Act
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), List.of(), grunnlag);
        // Assert
        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto -> {
            BeregningsgrunnlagPrStatusOgAndelDto beregningsgrunnlagPrStatusOgAndelDto = beregningsgrunnlagDto.getBeregningsgrunnlagPeriode().get(0).getBeregningsgrunnlagPrStatusOgAndel().get(0);
            assertThat(beregningsgrunnlagPrStatusOgAndelDto).isNotNull();
            assertThat(beregningsgrunnlagPrStatusOgAndelDto.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
            assertThat(beregningsgrunnlagPrStatusOgAndelDto.getSkalFastsetteGrunnlag()).isEqualTo(true);
        });

        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto -> {
            BeregningsgrunnlagPrStatusOgAndelDto beregningsgrunnlagPrStatusOgAndelDto = beregningsgrunnlagDto.getBeregningsgrunnlagPeriode().get(0).getBeregningsgrunnlagPrStatusOgAndel().get(1);
            assertThat(beregningsgrunnlagPrStatusOgAndelDto).isNotNull();
            assertThat(beregningsgrunnlagPrStatusOgAndelDto.getAktivitetStatus()).isEqualTo(AktivitetStatus.FRILANSER);
            assertThat(beregningsgrunnlagPrStatusOgAndelDto.getSkalFastsetteGrunnlag()).isEqualTo(false);
        });

        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto -> {
            BeregningsgrunnlagPrStatusOgAndelDto beregningsgrunnlagPrStatusOgAndelDto = beregningsgrunnlagDto.getBeregningsgrunnlagPeriode().get(0).getBeregningsgrunnlagPrStatusOgAndel().get(2);
            assertThat(beregningsgrunnlagPrStatusOgAndelDto).isNotNull();
            assertThat(beregningsgrunnlagPrStatusOgAndelDto.getAktivitetStatus()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
            assertThat(beregningsgrunnlagPrStatusOgAndelDto.getSkalFastsetteGrunnlag()).isEqualTo(true);
        });
    }

    @Test
    public void skalSetteBeregningsgrunnlagPrStatusOgAndelDtoForArbeidstakerNårSammenligningsTypeErATFL() {
        // Arrange
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        lagBehandlingMedBgOgOpprettFagsakRelasjon(arbeidsgiver);
        // Act
        Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt = lagBeregningsgrunnlagDto(lagReferanseMedStp(behandlingReferanse), List.of(), grunnlag);
        // Assert
        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(beregningsgrunnlagDto -> {
            BeregningsgrunnlagPrStatusOgAndelDto beregningsgrunnlagPrStatusOgAndelDto = beregningsgrunnlagDto.getBeregningsgrunnlagPeriode().get(0).getBeregningsgrunnlagPrStatusOgAndel().get(0);
            assertThat(beregningsgrunnlagPrStatusOgAndelDto).isNotNull();
            assertThat(beregningsgrunnlagPrStatusOgAndelDto.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
            assertThat(beregningsgrunnlagPrStatusOgAndelDto.getSkalFastsetteGrunnlag()).isEqualTo(true);
        });
    }

    private BehandlingReferanse lagReferanseMedStp(BehandlingReferanse behandlingReferanse) {
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT).build();
        return behandlingReferanse.medSkjæringstidspunkt(skjæringstidspunkt);
    }

    private Optional<BeregningsgrunnlagDto> lagBeregningsgrunnlagDto(BehandlingReferanse ref, Collection<Inntektsmelding> inntektsmeldinger, BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        var ytelsespesifiktGrunnlag = new K9BeregningsgrunnlagInput();
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().medInntektsmeldinger(inntektsmeldinger).build();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, ytelsespesifiktGrunnlag).medBeregningsgrunnlagGrunnlag(grunnlag);
        return beregningsgrunnlagDtoTjeneste.lagBeregningsgrunnlagDto(input);
    }

    private void assertBeregningsgrunnlag(BigDecimal fastsattIKofakBer, Optional<BeregningsgrunnlagDto> beregningsgrunnlagDtoOpt) {
        assertThat(beregningsgrunnlagDtoOpt).hasValueSatisfying(grunnlagDto -> {
            assertThat(grunnlagDto).isNotNull();
            assertThat(grunnlagDto.getSkjaeringstidspunktBeregning()).as("skjæringstidspunkt").isEqualTo(SKJÆRINGSTIDSPUNKT);
            assertThat(grunnlagDto.getLedetekstAvkortet()).isNotNull();
            assertThat(grunnlagDto.getLedetekstBrutto()).isNotNull();
            assertThat(grunnlagDto.getHalvG().intValue()).isEqualTo(grunnbeløp.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP).intValue());
            BeregningsgrunnlagPeriodeDto periodeDto = grunnlagDto.getBeregningsgrunnlagPeriode().get(0);
            assertThat(periodeDto.getBeregningsgrunnlagPeriodeFom()).as("BeregningsgrunnlagPeriodeFom").isEqualTo(ANDEL_FOM);
            assertThat(periodeDto.getBeregningsgrunnlagPeriodeTom()).as("BeregningsgrunnlagPeriodeTom").isNull();
            BeregningsgrunnlagPrStatusOgAndelDto andelDto = periodeDto.getBeregningsgrunnlagPrStatusOgAndel().get(0);
            assertThat(andelDto.getBeregnetPrAar()).isEqualByComparingTo(fastsattIKofakBer);
            assertThat(andelDto.getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
            assertThat(andelDto.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        });
    }

    private void assertFastsattVerdier(Inntektskategori inntektskategoriSattIFaktaOmBeregning, int overstyrtForrigeGang, int nyInntektIFaktaOmBeregning, BeregningsgrunnlagDto beregningsgrunnlagDto) {
        List<BeregningsgrunnlagPeriodeDto> beregningsgrunnlagPeriodeDtoList = beregningsgrunnlagDto.getBeregningsgrunnlagPeriode();
        assertThat(beregningsgrunnlagPeriodeDtoList).hasSize(1);
        BeregningsgrunnlagPeriodeDto periodeDto = beregningsgrunnlagPeriodeDtoList.get(0);
        List<BeregningsgrunnlagPrStatusOgAndelDto> andelList = periodeDto.getBeregningsgrunnlagPrStatusOgAndel();
        assertThat(andelList).hasSize(1);
        BeregningsgrunnlagPrStatusOgAndelDto andelDto = andelList.get(0);
        assertThat(andelDto.getInntektskategori()).isEqualTo(inntektskategoriSattIFaktaOmBeregning);
        assertThat(andelDto.getAndelsnr()).isEqualTo(ANDELSNR);
        assertThat(andelDto.getAvkortetPrAar()).isNull();
        assertThat(andelDto.getRedusertPrAar()).isNull();
        assertThat(andelDto.getBruttoPrAar()).isEqualByComparingTo(BigDecimal.valueOf(nyInntektIFaktaOmBeregning));
        assertThat(andelDto.getBeregnetPrAar()).isEqualTo(BigDecimal.valueOf(nyInntektIFaktaOmBeregning));
        assertThat(andelDto.getOverstyrtPrAar()).isEqualTo(BigDecimal.valueOf(overstyrtForrigeGang));
        assertThat(andelDto.getBeregningsgrunnlagFom()).isEqualTo(ANDEL_FOM);
        assertThat(andelDto.getBeregningsgrunnlagTom()).isEqualTo(ANDEL_TOM);
        assertThat(andelDto.getArbeidsforhold()).isNotNull();
        assertThat(andelDto.getArbeidsforhold().getArbeidsgiverNavn()).isEqualTo(virksomhet.getNavn());
        assertThat(andelDto.getArbeidsforhold().getArbeidsgiverId()).isEqualTo(virksomhet.getOrgnr());
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag(TestScenarioBuilder scenario, Arbeidsgiver arbeidsgiver) {
        var beregningsgrunnlag = scenario.medBeregningsgrunnlag()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(grunnbeløp)
            .leggTilSammenligningsgrunnlag(SammenligningsgrunnlagPrStatus.builder()
                .medAvvikPromille(AVVIK_OVER_25_PROSENT)
                .medRapportertPrÅr(RAPPORTERT_PR_AAR)
                .medSammenligningsgrunnlagType(SammenligningsgrunnlagType.SAMMENLIGNING_ATFL_SN)
                .medSammenligningsperiode(SAMMENLIGNING_FOM, SAMMENLIGNING_TOM))
            .build();
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medHjemmel(Hjemmel.F_14_7_8_30)
            .build(beregningsgrunnlag);
        Sammenligningsgrunnlag.builder()
            .medSammenligningsperiode(SAMMENLIGNING_FOM, SAMMENLIGNING_TOM)
            .medRapportertPrÅr(RAPPORTERT_PR_AAR)
            .medAvvikPromille(AVVIK_OVER_25_PROSENT).build(beregningsgrunnlag);


        BeregningsgrunnlagPeriode bgPeriode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag);
        buildBgPrStatusOgAndel(bgPeriode, arbeidsgiver);
        return beregningsgrunnlag;
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlagMedFlereAndeler(TestScenarioBuilder scenario, Arbeidsgiver arbeidsgiver) {
        var beregningsgrunnlag = scenario.medBeregningsgrunnlag()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(grunnbeløp)
            .leggTilSammenligningsgrunnlag(SammenligningsgrunnlagPrStatus.builder()
                .medAvvikPromille(AVVIK_OVER_25_PROSENT)
                .medRapportertPrÅr(RAPPORTERT_PR_AAR)
                .medSammenligningsgrunnlagType(SammenligningsgrunnlagType.SAMMENLIGNING_AT)
                .medSammenligningsperiode(SAMMENLIGNING_FOM, SAMMENLIGNING_TOM))
            .leggTilSammenligningsgrunnlag(SammenligningsgrunnlagPrStatus.builder()
                .medAvvikPromille(AVVIK_UNDER_25_PROSENT)
                .medRapportertPrÅr(RAPPORTERT_PR_AAR)
                .medSammenligningsgrunnlagType(SammenligningsgrunnlagType.SAMMENLIGNING_FL)
                .medSammenligningsperiode(SAMMENLIGNING_FOM, SAMMENLIGNING_TOM))
            .leggTilSammenligningsgrunnlag(SammenligningsgrunnlagPrStatus.builder()
                .medAvvikPromille(AVVIK_OVER_25_PROSENT)
                .medRapportertPrÅr(RAPPORTERT_PR_AAR)
                .medSammenligningsgrunnlagType(SammenligningsgrunnlagType.SAMMENLIGNING_SN)
                .medSammenligningsperiode(SAMMENLIGNING_FOM, SAMMENLIGNING_TOM))
            .build();
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medHjemmel(Hjemmel.F_14_7_8_30)
            .build(beregningsgrunnlag);
        Sammenligningsgrunnlag.builder()
            .medSammenligningsperiode(SAMMENLIGNING_FOM, SAMMENLIGNING_TOM)
            .medRapportertPrÅr(RAPPORTERT_PR_AAR)
            .medAvvikPromille(AVVIK_OVER_25_PROSENT).build(beregningsgrunnlag);


        BeregningsgrunnlagPeriode bgPeriode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag);
        buildBgPrStatusOgAndelForMangeAndeler(bgPeriode, arbeidsgiver);
        return beregningsgrunnlag;
    }

    private void buildBgPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, Arbeidsgiver arbeidsgiver) {
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
            .builder()
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2))
            .medArbeidsgiver(arbeidsgiver);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medInntektskategori(INNTEKTSKATEGORI)
            .medAndelsnr(ANDELSNR)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBeregningsperiode(ANDEL_FOM, ANDEL_TOM)
            .medBeregnetPrÅr(BRUTTO_PR_AAR)
            .medAvkortetPrÅr(AVKORTET_PR_AAR)
            .medRedusertPrÅr(REDUSERT_PR_AAR)
            .medOverstyrtPrÅr(OVERSTYRT_PR_AAR)
            .build(beregningsgrunnlagPeriode);
    }

    private no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(ANDEL_FOM, null)
            .build(beregningsgrunnlag);
    }

    private void buildBgPrStatusOgAndelForMangeAndeler(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, Arbeidsgiver arbeidsgiver) {
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
            .builder()
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2))
            .medArbeidsgiver(arbeidsgiver);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medInntektskategori(INNTEKTSKATEGORI)
            .medAndelsnr(ANDELSNR)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBeregningsperiode(ANDEL_FOM, ANDEL_TOM)
            .medBeregnetPrÅr(BRUTTO_PR_AAR)
            .medAvkortetPrÅr(AVKORTET_PR_AAR)
            .medRedusertPrÅr(REDUSERT_PR_AAR)
            .medOverstyrtPrÅr(OVERSTYRT_PR_AAR)
            .build(beregningsgrunnlagPeriode);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medInntektskategori(INNTEKTSKATEGORI)
            .medAndelsnr(ANDELSNR+1)
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medBeregningsperiode(ANDEL_FOM, ANDEL_TOM)
            .medBeregnetPrÅr(BRUTTO_PR_AAR)
            .build(beregningsgrunnlagPeriode);

        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medInntektskategori(INNTEKTSKATEGORI)
            .medAndelsnr(ANDELSNR+3)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medPgi(PGI_SNITT, List.of())
            .build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagEntitet lagBehandlingMedBgOgOpprettFagsakRelasjon(Arbeidsgiver arbeidsgiver) {
        var scenario = TestScenarioBuilder.nyttScenario();

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS,
            BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);
        this.beregningAktiviteter = lagBeregningAktiviteter(scenario, arbeidsgiver);
        var beregningsgrunnlag = lagBeregningsgrunnlag(scenario, arbeidsgiver);
        behandlingReferanse = lagre(scenario);

        BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlagBuilder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medRegisterAktiviteter(beregningAktiviteter)
                .medBeregningsgrunnlag(beregningsgrunnlag).build(behandlingReferanse.getId(),  BeregningsgrunnlagTilstand.OPPRETTET);

        this.grunnlag = beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(),  BeregningsgrunnlagGrunnlagBuilder.oppdatere(Kopimaskin.deepCopy(beregningsgrunnlagGrunnlagBuilder)), BeregningsgrunnlagTilstand.OPPRETTET);
        this.opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID, Periode.of( SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.minusDays(1)), arbeidsgiver.getOrgnr());

        return beregningsgrunnlag;
    }

    private BeregningsgrunnlagEntitet lagBehandlingMedBgOgOpprettFagsakRelasjonFlereAndeler(Arbeidsgiver arbeidsgiver) {
        var scenario = TestScenarioBuilder.nyttScenario();

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS,
            BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);
        this.beregningAktiviteter = lagBeregningAktiviteter(scenario, arbeidsgiver);
        var beregningsgrunnlag = lagBeregningsgrunnlagMedFlereAndeler(scenario, arbeidsgiver);
        behandlingReferanse = lagre(scenario);

        BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlagBuilder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktiviteter)
            .medBeregningsgrunnlag(beregningsgrunnlag).build(behandlingReferanse.getId(),  BeregningsgrunnlagTilstand.OPPRETTET);

        this.grunnlag = beregningsgrunnlagRepository.lagre(behandlingReferanse.getId(),  BeregningsgrunnlagGrunnlagBuilder.oppdatere(Kopimaskin.deepCopy(beregningsgrunnlagGrunnlagBuilder)), BeregningsgrunnlagTilstand.OPPRETTET);
        this.opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID, Periode.of( SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.minusDays(1)), arbeidsgiver.getOrgnr());

        return beregningsgrunnlag;
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
       return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }

    private BeregningAktivitetAggregatEntitet lagBeregningAktiviteter(TestScenarioBuilder scenario, Arbeidsgiver arbeidsgiver) {
        BeregningAktivitetAggregatEntitet.Builder builder = scenario.medBeregningAktiviteter();
        return lagBeregningAktiviteter(builder, arbeidsgiver);
    }

    private BeregningAktivitetAggregatEntitet lagBeregningAktiviteter(BeregningAktivitetAggregatEntitet.Builder builder, Arbeidsgiver arbeidsgiver) {
        return builder.medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
            .leggTilAktivitet(BeregningAktivitetEntitet.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
                .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(ANDEL_FOM, ANDEL_TOM))
                .build())
            .build();
    }

    private BeregningsgrunnlagGrunnlagEntitet lagOpprettetBg() {
        Long behandlingId = behandlingReferanse.getId();
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId).dypKopi();
        BeregningsgrunnlagPrStatusOgAndel andel = bg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(andel)
            .medBeregnetPrÅr(null)
            .medInntektskategori(INNTEKTSKATEGORI)
            .medOverstyrtPrÅr(null)
            .medAvkortetPrÅr(null)
            .medRedusertPrÅr(null);

        var builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(bg);
        this.grunnlag = beregningsgrunnlagRepository.lagre(behandlingId, builder, BeregningsgrunnlagTilstand.OPPRETTET);
        return this.grunnlag;
    }


    private BeregningsgrunnlagGrunnlagEntitet lagBgMedRedigerteVerdierIFaktaOmBeregning(Integer beregnetPrÅr, Inntektskategori inntektskategori) {
        Long behandlingId = behandlingReferanse.getId();
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId).dypKopi();
        BeregningsgrunnlagPrStatusOgAndel andel = bg.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(andel)
            .medBeregnetPrÅr(BigDecimal.valueOf(beregnetPrÅr))
            .medInntektskategori(inntektskategori)
            .medFastsattAvSaksbehandler(true);

        var builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medRegisterAktiviteter(beregningAktiviteter)
                .medBeregningsgrunnlag(bg);
        this.grunnlag = beregningsgrunnlagRepository.lagre(behandlingId, builder,  BeregningsgrunnlagTilstand.KOFAKBER_UT);
        return this.grunnlag;
    }


    private BeregningsgrunnlagGrunnlagEntitet lagBgMedOverstyrteVerdierISteg100(Integer overstyrtPrÅr) {
        Long behandlingId = behandlingReferanse.getId();
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId).dypKopi();
        BeregningsgrunnlagPrStatusOgAndel andel = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(andel)
            .medRedusertPrÅr(REDUSERT_PR_AAR)
            .medAvkortetPrÅr(AVKORTET_PR_AAR)
            .medOverstyrtPrÅr(BigDecimal.valueOf(overstyrtPrÅr));
        this.grunnlag = beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
        return this.grunnlag;
    }

    private Personinfo lagPersoninfo() {
        Personinfo.Builder b = new Personinfo.Builder()
            .medNavn(PRIVATPERSON_NAVN)
            .medPersonIdent(new PersonIdent(PRIVATPERSON_IDENT))
            .medAktørId(AktørId.dummy())
            .medFødselsdato(FØDSELSDATO)
            .medKjønn(NavBrukerKjønn.KVINNE);
        return b.build();
    }

}
