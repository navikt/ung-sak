package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.util.BeregningsgrunnlagTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste.VurderManuellBehandling;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningInntektsmeldingTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Gradering;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;

public class RefusjonOgGraderingTjenesteTest {

    private static final String ORG_NUMMER = "991825827";

    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.of(2018, 9, 30);

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(repoRule.getEntityManager());

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);

    private FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste;

    private BeregningIAYTestUtil iayTestUtil = new BeregningIAYTestUtil(iayTjeneste);

    private BeregningInntektsmeldingTestUtil inntektsmeldingTestUtil = new BeregningInntektsmeldingTestUtil(inntektsmeldingTjeneste);

    private BeregningsgrunnlagTestUtil beregningTestUtil = new BeregningsgrunnlagTestUtil(beregningsgrunnlagRepository, iayTjeneste);

    private BehandlingReferanse behandlingReferanse;

    private Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet(ORG_NUMMER);
    private Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet("456456456456");

    private BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = mock(BeregningAktivitetAggregatEntitet.class);
    private List<BeregningAktivitetEntitet> aktivitetList = new ArrayList<>();

    @Before
    public void setup() {

        refusjonOgGraderingTjeneste = new FordelBeregningsgrunnlagTjeneste();

        when(beregningAktivitetAggregat.getBeregningAktiviteter()).thenReturn(aktivitetList);
        var scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    @Test
    public void returnererFalseOmArbeidsforholdStarterFørStp() {
        // Arrange
        String orgnr = "123456780";
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        setAktivitetFørStp(arbeidsgiver, null);

        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .build();
        BeregningsgrunnlagPeriode periode = lagPeriode(bg);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                .medArbeidsperiodeFom(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10))
                .medArbeidsperiodeTom(SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(1))
                .medArbeidsgiver(arbeidsgiver)
                .medRefusjonskravPrÅr(BigDecimal.valueOf(10000)))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(periode);
        // Act
        boolean tilkomEtter = refusjonOgGraderingTjeneste.erNyttArbeidsforhold(andel, beregningAktivitetAggregat);

        // Assert
        assertThat(tilkomEtter).isFalse();
    }

    @Test
    public void returnererTrueForArbeidsforholdSomStarterEtterSkjæringstidspunkt() {
        // Arrange
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .build();
        BeregningsgrunnlagPeriode periode2 = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(3), null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                .medArbeidsperiodeFom(SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(3))
                .medArbeidsperiodeTom(SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(5))
                .medArbeidsgiver(arbeidsgiver1)
                .medRefusjonskravPrÅr(BigDecimal.valueOf(10000)))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(periode2);
        // Act
        boolean tilkomEtter = refusjonOgGraderingTjeneste.erNyttArbeidsforhold(andel, beregningAktivitetAggregat);
        // Assert
        assertThat(tilkomEtter).isTrue();
    }

    @Test
    public void returnererTrueForFLMedGraderingSomTilkommer() {
        // Arrange
        var scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, 1000, periode1, false, null, BigDecimal.valueOf(10));
        setAktivitetFørStp(arbeidsgiver1, null);
        lagFLAndel(periode1);

        // Act
        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.FRILANSER)
            .medGradering(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusWeeks(18).minusDays(1), 50)
            .build());
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, aktivitetGradering);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).contains(VurderManuellBehandling.FL_ELLER_SN_TILKOMMER);
    }

    @Test
    public void returnererTrueForSNMedGraderingSomTilkommer() {
        // Arrange
        LocalDate fom = SKJÆRINGSTIDSPUNKT_BEREGNING;
        LocalDate tom = fom.plusWeeks(18).minusDays(1);
        var scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, 1000, periode1, false, null, BigDecimal.valueOf(10));
        setAktivitetFørStp(arbeidsgiver1, null);
        lagSNAndel(periode1);

        // Act
        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medGradering(fom, tom, 50)
            .build());

        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, aktivitetGradering);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).contains(VurderManuellBehandling.FL_ELLER_SN_TILKOMMER);
    }

    @Test
    public void returnererTrueForAAPMedRefusjonskravSomOverstigerInntekt() {
        // Arrange
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(),
            SKJÆRINGSTIDSPUNKT_BEREGNING, BigDecimal.valueOf(1000), BigDecimal.valueOf(10), LocalDateTime.now());
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, 1000, periode1, false, null, BigDecimal.valueOf(10));
        setAktivitetFørStp(arbeidsgiver1, null);
        lagAAPAndel(periode1);

        // Act
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).contains(VurderManuellBehandling.REFUSJON_STØRRE_ENN_OPPGITT_INNTEKT_OG_HAR_AAP);
    }

    @Test
    public void returnererFalseForAAPMedRefusjonskravSomIkkeOverstigerInntekt() {
        // Arrange
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(),
            SKJÆRINGSTIDSPUNKT_BEREGNING, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), LocalDateTime.now());
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, 1000, periode1, false, null, BigDecimal.valueOf(1000));
        setAktivitetFørStp(arbeidsgiver1, null);
        lagAAPAndel(periode1);

        // Act
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).isEmpty();
    }

    @Test
    public void returnererFalseOmBeregningsgrunnlagIkkjeHarPerioderMedPeriodeårsakerGraderingEllerEndringIRefusjon() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr));
        List<List<PeriodeÅrsak>> periodePeriodeÅrsaker = List.of(Collections.emptyList(), Collections.singletonList(PeriodeÅrsak.NATURALYTELSE_BORTFALT));
        List<LocalDateInterval> perioder = List.of(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusWeeks(2)),
            new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusWeeks(2).plusDays(1), null));
        beregningTestUtil.lagBeregningsgrunnlagForEndring(behandlingReferanse, SKJÆRINGSTIDSPUNKT_BEREGNING, periodePeriodeÅrsaker, perioder);

        // Act
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            beregningAktivitetAggregat, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).isEmpty();
    }

    @Test
    public void returnererFalseForNyInntektsmeldingUtenRefusjonskrav() {
        // Arrange
        var arbId = InternArbeidsforholdRef.nyRef();
        String orgnr = "123456780";
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(1),
            SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(5).minusDays(2), arbId, Arbeidsgiver.virksomhet(orgnr));
        List<List<PeriodeÅrsak>> opprinneligePeriodeÅrsaker = Collections.singletonList(Collections.emptyList());
        List<LocalDateInterval> opprinneligePerioder = Collections.singletonList(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingReferanse, SKJÆRINGSTIDSPUNKT_BEREGNING, opprinneligePerioder,
            opprinneligePeriodeÅrsaker);
        List<List<PeriodeÅrsak>> periodePeriodeÅrsaker = List.of(Collections.emptyList(), Collections.singletonList(PeriodeÅrsak.GRADERING));
        List<LocalDateInterval> perioder = List.of(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusWeeks(2)),
            new LocalDateInterval(SKJÆRINGSTIDSPUNKT_BEREGNING.plusWeeks(2).plusDays(1), null));
        beregningTestUtil.lagBeregningsgrunnlagForEndring(behandlingReferanse, SKJÆRINGSTIDSPUNKT_BEREGNING, periodePeriodeÅrsaker, perioder);
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr, arbId, SKJÆRINGSTIDSPUNKT_BEREGNING, LocalDateTime.now());
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr, arbId, SKJÆRINGSTIDSPUNKT_BEREGNING, LocalDateTime.now().plusSeconds(1));
        setAktivitetFørStp(arbeidsgiver1, arbId);
        setAktivitetFørStp(arbeidsgiver2, arbId);

        // Act
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingReferanse.getId()),
            beregningAktivitetAggregat, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).isEmpty();
    }

    // Gradering: Ja
    // Refusjon: Nei
    // Tilkom etter skjæringstidspunktet: Ja
    // Returnerer true

    @Test
    public void returnererTrueForGraderingOgArbeidsforholdetTilkomEtterSkjæringstidpunktet() {
        // Arrange
        LocalDate fom = SKJÆRINGSTIDSPUNKT_BEREGNING;
        LocalDate tom = fom.plusWeeks(18).minusDays(1);
        var arbId = InternArbeidsforholdRef.nyRef();
        var scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, null, periode1, true, null, BigDecimal.valueOf(10));

        // Act
        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbeidsgiver(arbeidsgiver1)
            .medArbeidsforholdRef(arbId)
            .medGradering(fom, tom, 50)
            .build());
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, aktivitetGradering);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).contains(VurderManuellBehandling.NYTT_ARBEIDSFORHOLD);
    }
    // Gradering: Ja
    // Refusjon: Nei
    // Tilkom etter skjæringstidspunktet: Nei
    // Total refusjon under 6G
    // Returnerer false

    @Test
    public void returnererTrueForGraderingGjeldendeBruttoBGStørreEnnNullBeregningsgrunnlagsandelAvkortetTilNull() {
        // Arrange
        var arbId1 = InternArbeidsforholdRef.nyRef();
        String orgnr1 = "123456780";
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr1, arbId1, SKJÆRINGSTIDSPUNKT_BEREGNING, LocalDateTime.now());
        Gradering gradering = new Gradering(SKJÆRINGSTIDSPUNKT_BEREGNING.plusWeeks(2).plusDays(1), null, BigDecimal.valueOf(50));
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, orgnr1, arbId1,
            SKJÆRINGSTIDSPUNKT_BEREGNING, Collections.singletonList(gradering), 0, LocalDateTime.now().plusSeconds(1));
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, 0, periode1, false, null, BigDecimal.valueOf(10));
        setAktivitetFørStp(arbeidsgiver1, arbId1);

        // Act
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).isEmpty();
    }
    // Gradering: Ja
    // Refusjon: Nei
    // Total refusjon større enn 6G for alle arbeidsforhold
    // Tilkom etter skjæringstidspunktet: Nei
    // Returnerer True

    @Test
    public void returnererTrueForGraderingGjeldendeBruttoBGLikNullTotalRefusjonStørreEnn6G() {
        // Arrange
        LocalDate fom = SKJÆRINGSTIDSPUNKT_BEREGNING;
        LocalDate tom = fom.plusWeeks(18).minusDays(1);
        var scenario = TestScenarioBuilder.nyttScenario();

        int seksG = beregningTestUtil.getGrunnbeløp(fom).multiply(BigDecimal.valueOf(6)).intValue();
        int refusjon2PerÅr = seksG + 12;
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver2.getIdentifikator(), arbId1, SKJÆRINGSTIDSPUNKT_BEREGNING, refusjon2PerÅr/12,
            LocalDateTime.now());
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, null, periode1, false, null, BigDecimal.valueOf(10));
        lagAndel(arbeidsgiver2, refusjon2PerÅr, periode1, false, null, BigDecimal.valueOf(10));
        setAktivitetFørStp(arbeidsgiver1, arbId1);
        setAktivitetFørStp(arbeidsgiver2, arbId2);

        // Act
        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbeidsgiver(arbeidsgiver1)
            .medArbeidsforholdRef(arbId1)
            .medGradering(fom, tom, 50)
            .build());
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, aktivitetGradering);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).contains(VurderManuellBehandling.TOTALT_REFUSJONSKRAV_STØRRE_ENN_6G);
    }
    // Gradering: Ja
    // Refusjon: Nei
    // Total refusjon mindre enn 6G for alle arbeidsforhold
    // Tilkom etter skjæringstidspunktet: Nei
    // Returnerer True

    @Test
    public void returnererFalseForGraderingGjeldendeBruttoBGLikNullTotalRefusjonMindreEnn6G() {
        // Arrange
        int seksG = beregningTestUtil.getGrunnbeløp(SKJÆRINGSTIDSPUNKT_BEREGNING).multiply(BigDecimal.valueOf(6)).intValue();
        int refusjon2 = seksG - 12;
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver2.getIdentifikator(), arbId2, SKJÆRINGSTIDSPUNKT_BEREGNING, refusjon2 / 12,
            LocalDateTime.now());
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(), arbId1, SKJÆRINGSTIDSPUNKT_BEREGNING,
            LocalDateTime.now().plusSeconds(1));
        Gradering gradering = new Gradering(SKJÆRINGSTIDSPUNKT_BEREGNING.plusWeeks(2).plusDays(1), null, BigDecimal.valueOf(50));
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(), arbId1,
            SKJÆRINGSTIDSPUNKT_BEREGNING, Collections.singletonList(gradering), LocalDateTime.now().plusSeconds(2));
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, null, periode1, false, null, BigDecimal.valueOf(10));
        lagAndel(arbeidsgiver2, refusjon2 / 12, periode1, false, null, BigDecimal.valueOf(10));
        setAktivitetFørStp(arbeidsgiver1, arbId1);
        setAktivitetFørStp(arbeidsgiver2, arbId2);

        // Act
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).isEmpty();
    }
    // Gradering: Ja
    // Refusjon: Ja
    // Tilkom etter skjæringstidspunktet: Ja
    // Returnerer True

    @Test
    public void returnererTrueForGraderingOgRefusjonUtenGjeldendeBG() {
        // Arrange
        var arbId1 = InternArbeidsforholdRef.nyRef();
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(), arbId1, SKJÆRINGSTIDSPUNKT_BEREGNING, LocalDateTime.now());
        Gradering gradering = new Gradering(SKJÆRINGSTIDSPUNKT_BEREGNING.plusWeeks(2).plusDays(1), null, BigDecimal.valueOf(50));
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(), arbId1,
            SKJÆRINGSTIDSPUNKT_BEREGNING, Collections.singletonList(gradering), 100, LocalDateTime.now().plusSeconds(1));
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, 100, periode1, true, null, BigDecimal.valueOf(10));
        // Act
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).contains(VurderManuellBehandling.NYTT_ARBEIDSFORHOLD);
    }
    // Gradering: Ja
    // Refusjon: Ja
    // Tilkom etter skjæringstidspunktet: Nei
    // Returnerer False

    @Test
    public void returnererFalseForGraderingOgRefusjon() {
        // Arrange
        var arbId1 = InternArbeidsforholdRef.nyRef();
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(), arbId1, SKJÆRINGSTIDSPUNKT_BEREGNING, LocalDateTime.now());
        Gradering gradering = new Gradering(SKJÆRINGSTIDSPUNKT_BEREGNING.plusWeeks(2).plusDays(1), null, BigDecimal.valueOf(50));
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(), arbId1,
            SKJÆRINGSTIDSPUNKT_BEREGNING, Collections.singletonList(gradering), 100, LocalDateTime.now().plusSeconds(1));
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, 100, periode1, false, null, BigDecimal.valueOf(10));
        setAktivitetFørStp(arbeidsgiver1, arbId1);

        // Act
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).isEmpty();
    }
    // Gradering: Nei
    // Refusjon: Ja
    // Tilkom etter skjæringstidspunktet: Ja
    // Returnerer True

    @Test
    public void returnererTrueForRefusjonArbfholdTilkomEtterStp() {
        // Arrange
        var arbId1 = InternArbeidsforholdRef.nyRef();
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(), arbId1, SKJÆRINGSTIDSPUNKT_BEREGNING, 1000,
            LocalDateTime.now());
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(), arbId1, SKJÆRINGSTIDSPUNKT_BEREGNING, 1000,
            LocalDateTime.now().plusSeconds(1));
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, 1000, periode1, true, null, BigDecimal.valueOf(10));

        // Act
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).contains(VurderManuellBehandling.NYTT_ARBEIDSFORHOLD);
    }
    // Gradering: Nei
    // Refusjon: Ja
    // Tilkom etter skjæringstidspunktet: Nei
    // Returnerer False

    @Test
    public void returnererFalseForRefusjonGjeldendeBruttoBGStørreEnn0() {
        // Arrange
        var arbId1 = InternArbeidsforholdRef.nyRef();
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(), arbId1, SKJÆRINGSTIDSPUNKT_BEREGNING, 1000,
            LocalDateTime.now());
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(), arbId1, SKJÆRINGSTIDSPUNKT_BEREGNING, 1000,
            LocalDateTime.now().plusSeconds(1));
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, 1000, periode1, false, null, BigDecimal.valueOf(10));
        setAktivitetFørStp(arbeidsgiver1, arbId1);

        // Act
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, AktivitetGradering.INGEN_GRADERING);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).isEmpty();
    }


    @Test
    public void returnererTrueNårGradertNæringMedArbeidstakerTotalRefusjonUnder6GOgBGOver6G() {
        // Arrange
        var scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        var arbId1 = InternArbeidsforholdRef.nyRef();
        BigDecimal seksG = beregningTestUtil.getGrunnbeløp(SKJÆRINGSTIDSPUNKT_BEREGNING).multiply(BigDecimal.valueOf(6));
        int refusjon = (seksG.intValue() - 1) / 12;
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver1.getIdentifikator(), arbId1, SKJÆRINGSTIDSPUNKT_BEREGNING, refusjon,
            LocalDateTime.now());
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagAndel(arbeidsgiver1, refusjon*12, periode1, false, null, seksG.add(BigDecimal.ONE));
        lagSNAndel(periode1);
        setAktivitetFørStp(arbeidsgiver1, arbId1);
        setAktivitetFørStp(OpptjeningAktivitetType.NÆRING);
        LocalDate fom = SKJÆRINGSTIDSPUNKT_BEREGNING;
        LocalDate tom = fom.plusWeeks(18).minusDays(1);

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .leggTilGradering(fom, tom, BigDecimal.valueOf(50))
            .build());

        // Act
        List<Inntektsmelding> inntektsmeldinger = hentAlleInntektsmeldinger();
        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = refusjonOgGraderingTjeneste.vurderManuellBehandling(bg, beregningAktivitetAggregat, aktivitetGradering, inntektsmeldinger);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG.get()).isEqualTo(VurderManuellBehandling.GRADERT_ANDEL_SOM_VILLE_HA_BLITT_AVKORTET_TIL_0);
    }

    @Test
    public void returnererTrueForSNMedGraderingUtenBeregningsgrunnlag() {
        // Arrange
        LocalDate fom = SKJÆRINGSTIDSPUNKT_BEREGNING;
        LocalDate tom = fom.plusWeeks(18).minusDays(1);
        var scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        BeregningsgrunnlagEntitet bg = lagBg();
        BeregningsgrunnlagPeriode periode1 = lagPeriode(bg);
        lagSNAndel(periode1, 0);
        setAktivitetFørStp(OpptjeningAktivitetType.NÆRING);

        // Act
        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .leggTilGradering(fom, tom, BigDecimal.valueOf(50))
            .build());

        Optional<VurderManuellBehandling> manuellBehandlingForEndringAvBG = vurderManuellBehandling(bg, beregningAktivitetAggregat, aktivitetGradering);

        // Assert
        assertThat(manuellBehandlingForEndringAvBG).contains(VurderManuellBehandling.FORESLÅTT_BG_PÅ_GRADERT_ANDEL_ER_0);
    }

    private void setAktivitetFørStp(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        List<BeregningAktivitetEntitet> aktiviteterFørStp = Collections.singletonList(BeregningAktivitetEntitet.builder()
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10), TIDENES_ENDE))
            .medArbeidsgiver(arbeidsgiver).medArbeidsforholdRef(arbeidsforholdRef).build());
        aktivitetList.addAll(aktiviteterFørStp);
    }

    private void setAktivitetFørStp(OpptjeningAktivitetType type) {
        List<BeregningAktivitetEntitet> aktiviteterFørStp = Collections.singletonList(BeregningAktivitetEntitet.builder()
            .medOpptjeningAktivitetType(type)
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(10), TIDENES_ENDE)).build());
        aktivitetList.addAll(aktiviteterFørStp);
    }

    private Optional<VurderManuellBehandling> vurderManuellBehandling(BeregningsgrunnlagEntitet bg, BeregningAktivitetAggregatEntitet beregningAktivitetAggregat, AktivitetGradering aktivitetGradering) {
        return refusjonOgGraderingTjeneste.vurderManuellBehandling(bg, beregningAktivitetAggregat, aktivitetGradering, hentAlleInntektsmeldinger());
    }

    private Optional<String> finnRefFraInntektsmelding(Arbeidsgiver arbeidsgiver) {
        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldingerBeregning(behandlingReferanse,
            SKJÆRINGSTIDSPUNKT_BEREGNING);
        Optional<String> internArbeidsforholdReferanse = inntektsmeldinger.stream()
            .filter(im -> im.getArbeidsgiver().equals(arbeidsgiver))
            .findFirst().map(Inntektsmelding::getArbeidsforholdRef)
            .map(InternArbeidsforholdRef::getReferanse);
        return internArbeidsforholdReferanse;
    }

    private BeregningsgrunnlagPeriode lagPeriode(BeregningsgrunnlagEntitet bg) {
        return BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, null)
            .build(bg);
    }

    private BeregningsgrunnlagEntitet lagBg() {
        BigDecimal grunnbeløp = beregningTestUtil.getGrunnbeløp(SKJÆRINGSTIDSPUNKT_BEREGNING);
        return BeregningsgrunnlagEntitet.builder().leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .medGrunnbeløp(grunnbeløp)
            .build();
    }

    private void lagAndel(Arbeidsgiver arbeidsgiver, Integer refusjon2, BeregningsgrunnlagPeriode periode1, boolean tilkomEtter, BigDecimal overstyrtPrÅr,
                          BigDecimal beregnetPrÅr) {
        String arbeidsforholdRef = finnRefFraInntektsmelding(arbeidsgiver).orElse(null);
        BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBeregnetPrÅr(beregnetPrÅr)
            .medOverstyrtPrÅr(overstyrtPrÅr)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdRef(arbeidsforholdRef)
                .medRefusjonskravPrÅr(refusjon2 == null ? null : BigDecimal.valueOf(refusjon2))
                .medArbeidsperiodeFom(tilkomEtter ? periode1.getBeregningsgrunnlagPeriodeFom() : SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(12)))
            .build(periode1);
    }

    private void lagAAPAndel(BeregningsgrunnlagPeriode periode1) {
        BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)
            .build(periode1);
    }

    private void lagFLAndel(BeregningsgrunnlagPeriode periode1) {
        BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.FRILANSER)
            .build(periode1);
    }

    private void lagSNAndel(BeregningsgrunnlagPeriode periode1) {
        lagSNAndel(periode1, 10);
    }

    private void lagSNAndel(BeregningsgrunnlagPeriode periode1, int beregnetPrÅr) {
        BeregningsgrunnlagPrStatusOgAndel.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medBeregnetPrÅr(BigDecimal.valueOf(beregnetPrÅr))
            .build(periode1);
    }

    private List<Inntektsmelding> hentAlleInntektsmeldinger() {
        return iayTjeneste.finnGrunnlag(behandlingReferanse.getBehandlingId())
            .orElse(InntektArbeidYtelseGrunnlagBuilder.nytt().build()).getInntektsmeldinger().map(InntektsmeldingAggregat::getAlleInntektsmeldinger).orElse(Collections.emptyList());
    }


}
