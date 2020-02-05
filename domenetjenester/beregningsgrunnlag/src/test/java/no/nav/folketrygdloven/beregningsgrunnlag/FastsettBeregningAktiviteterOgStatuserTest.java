package no.nav.folketrygdloven.beregningsgrunnlag;

import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningAktiviteterFraVLTilRegel.INGEN_AKTIVITET_MELDING;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBGSkjæringstidspunktOgStatuserFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBGStatuserFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter.OpptjeningPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelseBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori;
import no.nav.k9.kodeverk.arbeidsforhold.InntektPeriodeType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningSatsType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.organisasjon.VirksomhetType;
import no.nav.k9.sak.typer.JournalpostId;

public class FastsettBeregningAktiviteterOgStatuserTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.APRIL, 10);
    private static final LocalDate FØRSTE_UTTAKSDAG = SKJÆRINGSTIDSPUNKT_OPPTJENING.plusYears(1);
    private static final LocalDate DAGEN_FØR_SFO = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1);

    private static final String ORG_NUMMER = "915933149";
    private static final String ORG_NUMMER2 = "974760673";
    private static final String ORG_NUMMER_MED_FLERE_ARBEIDSFORHOLD = ORG_NUMMER;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final EntityManager entityManager = repoRule.getEntityManager();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(entityManager);

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private FastsettBeregningAktiviteter fastsettBeregningAktiviteter = new FastsettBeregningAktiviteter();
    private FastsettSkjæringstidspunktOgStatuser fastsettSkjæringstidspunktOgStatuser;

    private BehandlingReferanse behandlingReferanse;

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
    private BeregningIAYTestUtil iayTestUtil;

    private final AtomicLong journalpostIdInc = new AtomicLong(123L);
    private AbstractTestScenario<?> scenario;

    @Before
    public void setup() {
        iayTestUtil = new BeregningIAYTestUtil(iayTjeneste);
        var oversetterFraRegelSkjæringstidspunkt = new MapBGSkjæringstidspunktOgStatuserFraRegelTilVL(repositoryProvider.getBeregningsgrunnlagRepository()
        );
        fastsettSkjæringstidspunktOgStatuser = new FastsettSkjæringstidspunktOgStatuser(
            oversetterFraRegelSkjæringstidspunkt, new MapBGStatuserFraVLTilRegel());
        scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    @Test
    public void testForIngenOpptjeningsaktiviteter_exception() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(2), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(13), arbId1);

        // Assert
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(CoreMatchers.containsString(INGEN_AKTIVITET_MELDING));

        // Act
        act();
    }

    private BeregningsgrunnlagEntitet act() {
        return act(new OpptjeningAktiviteter());
    }

    private BeregningsgrunnlagEntitet act(OpptjeningAktiviteter opptjeningAktiviteter) {
        return act(opptjeningAktiviteter, List.of());
    }

    private BeregningsgrunnlagEntitet act(OpptjeningAktiviteter opptjeningAktiviteter, Collection<Inntektsmelding> inntektsmeldinger) {
        var ref = lagReferanseMedStp();
        var input = lagBeregningsgrunnlagInput(ref, opptjeningAktiviteter, inntektsmeldinger);
        var beregningAktivitetAggregat = fastsettBeregningAktiviteter.fastsettAktiviteter(input);
        return fastsettSkjæringstidspunktOgStatuser.fastsett(ref, beregningAktivitetAggregat, input.getIayGrunnlag());
    }

    private BeregningsgrunnlagInput lagBeregningsgrunnlagInput(BehandlingReferanse ref, OpptjeningAktiviteter opptjeningAktiviteter,
                                                               Collection<Inntektsmelding> inntektsmeldinger) {
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.finnGrunnlag(behandlingReferanse.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter, AktivitetGradering.INGEN_GRADERING, null);
        return input;
    }

    private BehandlingReferanse lagReferanseMedStp() {
        return behandlingReferanse.medSkjæringstidspunkt(Skjæringstidspunkt.builder()
            .medFørsteUttaksdato(FØRSTE_UTTAKSDAG)
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .build());
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedUbruttAktivitet() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), DAGEN_FØR_SFO, arbId1);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserGrunnbeløp(FØRSTE_UTTAKSDAG, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedAvbruttAktivitet() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(3), arbId1);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(3).plusDays(1), grunnlag);
        verifiserGrunnbeløp(FØRSTE_UTTAKSDAG.minusWeeks(3).plusDays(1), grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedLangvarigMilitærtjeneste() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2));
        var opptj2 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(3), arbId1);

        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(3).plusDays(1), grunnlag);
        verifiserGrunnbeløp(FØRSTE_UTTAKSDAG, grunnlag);

        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedKortvarigMilitærtjeneste() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(4),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2));
        var opptj2 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(5), arbId1);
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(5).plusDays(1), grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedKortvarigArbeidsforhold() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(4), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2), arbId1);
        var opptj2 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2));
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2).plusDays(1), grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForMilitærMedAndreAktiviteterIOpptjeningsperioden() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), arbId1);
        var opptj2 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), DAGEN_FØR_SFO);
        var opptj3 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1).plusDays(1), FagsakYtelseType.ARBEIDSAVKLARINGSPENGER, null);
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2, opptj3));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1).plusDays(2), grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
    }

    @Test
    public void testSkjæringstidspunktForMilitærUtenAndreAktiviteter() {
        // Arrange
        var opptj1 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING);

        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.MILITÆR_ELLER_SIVIL);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.MILITÆR_ELLER_SIVIL);
    }

    @Test
    public void testSkjæringstidspunktForKombinertArbeidstakerOgFrilanser() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), DAGEN_FØR_SFO, arbId1);
        var opptj2 = lagFrilansOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO);
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.KOMBINERT_AT_FL);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.FRILANSER);
    }

    @Test
    public void testSkjæringstidspunktForFlereFrilansaktiviteter() {
        // Arrange
        var opptj1 = lagFrilansOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO);
        var opptj2 = lagFrilansOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(6), DAGEN_FØR_SFO);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.FRILANSER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.FRILANSER);
    }

    @Test
    public void testSkjæringstidspunktForFlereArbeidsforholdIUlikeVirksomheter() {
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO, arbId1);
        var opptj2 = lagArbeidOgOpptjening(ORG_NUMMER2, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(6), DAGEN_FØR_SFO, arbId2);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForFlereArbeidsforholdISammeVirksomhet() {
        // Arrange
        String orgnr = ORG_NUMMER_MED_FLERE_ARBEIDSFORHOLD;
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        var arbId3 = InternArbeidsforholdRef.nyRef();

        Periode periode = Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO);
        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, periode, orgnr, arbId1),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, periode, orgnr, arbId2),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, periode, orgnr, arbId3));

        var inntektsmeldinger = opprettInntektsmelding(Arbeidsgiver.virksomhet(orgnr), arbId1, arbId2, arbId3);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(opptjeningAktiviteter, inntektsmeldinger);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForKombinertArbeidstakerOgSelvstendig() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2), arbId1);
        var opptj2 = lagNæringOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO);
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.KOMBINERT_AT_SN);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedSykepengerOgArbeidsforhold() {
        // Arrange
        var ytelseStørrelse1 = lagYtelseStørrelse(ORG_NUMMER);
        var ytelseStørrelse2 = YtelseStørrelseBuilder.ny()
            .medBeløp(BigDecimal.TEN)
            .medHyppighet(InntektPeriodeType.MÅNEDLIG)
            .build();

        leggTilAktørytelse(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
            RelatertYtelseTilstand.LØPENDE, behandlingReferanse.getSaksnummer().getVerdi(), FagsakYtelseType.SYKEPENGER,
            Collections.singletonList(ytelseStørrelse1), Arbeidskategori.ARBEIDSTAKER, false);
        leggTilAktørytelse(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2).plusDays(1), DAGEN_FØR_SFO,
            RelatertYtelseTilstand.LØPENDE, behandlingReferanse.getSaksnummer().getVerdi(), FagsakYtelseType.SYKEPENGER,
            Collections.singletonList(ytelseStørrelse2), Arbeidskategori.ARBEIDSTAKER, false);

        var opptj1 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID,
            Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(5), null), ORG_NUMMER2);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForDagpengemottakerMedSykepenger() {
        // Arrange
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2),
            FagsakYtelseType.DAGPENGER, null);
        var opptj2 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO, FagsakYtelseType.SYKEPENGER, ORG_NUMMER);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.DAGPENGER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.DAGPENGER);
    }

    @Test
    public void testSkjæringstidspunktForAAPmottakerMedSykepenger() {
        // Arrange
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2),
            FagsakYtelseType.ARBEIDSAVKLARINGSPENGER, null);
        var opptj2 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO, FagsakYtelseType.SYKEPENGER, ORG_NUMMER);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserGrunnbeløp(FØRSTE_UTTAKSDAG, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
    }

    @Test
    public void testPermisjonPåSkjæringstidspunktOpptjening() {
        // Assert
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);
        InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(arbeidsforholdRef);

        LocalDate permisjonFom = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1);
        LocalDate permisjonTom = SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(1);
        Permisjon permisjon = yrkesaktivitetBuilder.getPermisjonBuilder()
            .medPeriode(permisjonFom, permisjonTom)
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
            .medProsentsats(BigDecimal.valueOf(100))
            .build();
        yrkesaktivitetBuilder.leggTilPermisjon(permisjon);

        Periode opptjeningPeriode = Periode.of(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), permisjonFom.minusDays(1));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ORG_NUMMER);

        // Act
        BehandlingReferanse ref = lagReferanseMedStp();
        var input = lagBeregningsgrunnlagInput(ref, opptjeningAktiviteter, List.of());
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = fastsettBeregningAktiviteter.fastsettAktiviteter(input);

        BeregningsgrunnlagEntitet beregningsgrunnlag = fastsettSkjæringstidspunktOgStatuser.fastsett(ref, beregningAktivitetAggregat, input.getIayGrunnlag());

        // Assert
        assertThat(beregningsgrunnlag.getSkjæringstidspunkt()).isEqualTo(permisjonFom);
        assertThat(beregningAktivitetAggregat.getBeregningAktiviteter()).hasSize(1);
        BeregningAktivitetEntitet ba = beregningAktivitetAggregat.getBeregningAktiviteter().get(0);
        assertThat(ba.getArbeidsgiver()).isEqualTo(arbeidsgiver);
        assertThat(ba.getPeriode().getFomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1));
        assertThat(ba.getPeriode().getTomDato()).isEqualTo(permisjonFom.minusDays(1));
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedAlleAktiviteterUnntattTYogAAP() {
        var arbId1 = InternArbeidsforholdRef.nyRef();
        // Arrange
        var opptj0 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2), arbId1);
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO, FagsakYtelseType.DAGPENGER, null);
        var opptj2 = lagFrilansOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj3 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj4 = lagNæringOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj5 = lagAnnenAktivitetMedOpptjening(ArbeidType.VENTELØNN_VARTPENGER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj6 = lagAnnenAktivitetMedOpptjening(ArbeidType.ETTERLØNN_SLUTTPAKKE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj0, opptj1, opptj2, opptj3, opptj4, opptj5, opptj6));

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(opptjeningAktiviteter);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.KOMBINERT_AT_FL_SN, AktivitetStatus.DAGPENGER);

        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.FRILANSER,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, AktivitetStatus.DAGPENGER, AktivitetStatus.ARBEIDSTAKER,
            AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForDagpengemottakerMedSykepengerMedFørsteUttaksdagEtterGrunnbeløpEndring() {
        // Arrange
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2),
            FagsakYtelseType.DAGPENGER, null);
        var opptj2 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO, FagsakYtelseType.SYKEPENGER, ORG_NUMMER);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserGrunnbeløp(FØRSTE_UTTAKSDAG, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.DAGPENGER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.DAGPENGER);
    }

    private List<Inntektsmelding> opprettInntektsmelding(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef... arbIdListe) {
        List<Inntektsmelding> inntektsmeldinger = new ArrayList<>();
        for (var arbId : arbIdListe) {
            var im = InntektsmeldingBuilder.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medInnsendingstidspunkt(LocalDateTime.now())
                .medArbeidsforholdId(arbId)
                .medBeløp(BigDecimal.valueOf(100000)).medJournalpostId(new JournalpostId(journalpostIdInc.incrementAndGet()))
                .medStartDatoPermisjon(LocalDate.now());

            inntektsmeldinger.add(im.build());
        }

        return inntektsmeldinger;
    }

    private void verifiserSkjæringstidspunkt(LocalDate skjæringstidspunkt, BeregningsgrunnlagEntitet grunnlag) {
        assertThat(grunnlag.getSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt);
    }

    private void verifiserGrunnbeløp(LocalDate førsteUttaksdag, BeregningsgrunnlagEntitet grunnlag) {
        long gVerdi = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, førsteUttaksdag).getVerdi();
        assertThat(grunnlag.getGrunnbeløp().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(gVerdi));
    }

    private void verifiserBeregningsgrunnlagPerioder(BeregningsgrunnlagEntitet grunnlag, AktivitetStatus... expectedArray) {
        assertThat(grunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode bgPeriode = grunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<AktivitetStatus> actualList = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(BeregningsgrunnlagPrStatusOgAndel::getAktivitetStatus).collect(Collectors.toList());
        assertThat(actualList).containsOnly(expectedArray);
        bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(this::erArbeidstakerEllerFrilans)
            .forEach(this::verifiserBeregningsperiode);
        assertThat(actualList).hasSameSizeAs(expectedArray);
    }

    private boolean erArbeidstakerEllerFrilans(BeregningsgrunnlagPrStatusOgAndel bgpsa) {
        return (AktivitetStatus.ARBEIDSTAKER.equals(bgpsa.getAktivitetStatus()))
            || (AktivitetStatus.FRILANSER.equals(bgpsa.getAktivitetStatus()));
    }

    private void verifiserBeregningsperiode(BeregningsgrunnlagPrStatusOgAndel bgpsa) {
        assertThat(bgpsa.getBeregningsperiodeFom()).isNotNull();
        assertThat(bgpsa.getBeregningsperiodeTom()).isNotNull();
    }

    private void verifiserAktivitetStatuser(BeregningsgrunnlagEntitet grunnlag, AktivitetStatus... expectedArray) {
        List<AktivitetStatus> actualList = grunnlag.getAktivitetStatuser().stream()
            .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus).collect(Collectors.toList());
        assertThat(actualList).containsOnly(expectedArray);
    }

    private YtelseStørrelse lagYtelseStørrelse(String orgnummer) {
        return YtelseStørrelseBuilder.ny()
            .medBeløp(BigDecimal.TEN)
            .medHyppighet(InntektPeriodeType.MÅNEDLIG)
            .medVirksomhet(orgnummer).build();
    }

    private OpptjeningPeriode lagArbeidOgOpptjening(String orgNummer, LocalDate fom, LocalDate tom, InternArbeidsforholdRef arbId) {
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING,
            fom, tom, arbId, Arbeidsgiver.virksomhet(orgNummer));
        return OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, Periode.of(fom, tom), orgNummer);
    }

    private OpptjeningPeriode lagFrilansOgOpptjening(LocalDate fom, LocalDate tom) {
        iayTestUtil.byggPåOppgittOpptjeningForFL(false, Collections.singletonList(Periode.of(fom, tom)));

        return OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.FRILANS, Periode.of(fom, tom));
    }

    private OpptjeningPeriode lagNæringOgOpptjening(LocalDate fom, LocalDate tom) {
        iayTestUtil.byggPåOppgittOpptjeningForSN(SKJÆRINGSTIDSPUNKT_OPPTJENING, false, VirksomhetType.ANNEN, Collections.singleton(Periode.of(fom, tom)));
        return OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.NÆRING, Periode.of(fom, tom), null);
    }

    private OpptjeningPeriode lagAnnenAktivitetMedOpptjening(ArbeidType arbeidType, LocalDate fom, LocalDate tom) {
        iayTestUtil.byggPåOppgittOpptjeningAnnenAktivitet(arbeidType, fom, tom);
        return OpptjeningAktiviteter.nyPeriode(utledOpptjeningAktivitetType(arbeidType), Periode.of(fom, tom));
    }

    private OpptjeningPeriode lagYtelseMedOpptjening(LocalDate fom, LocalDate tom, FagsakYtelseType relatertYtelseType, String orgnr) {
        leggTilAktørytelse(behandlingReferanse, fom, tom, RelatertYtelseTilstand.LØPENDE, behandlingReferanse.getSaksnummer().getVerdi(),
            relatertYtelseType, Collections.singletonList(lagYtelseStørrelse(orgnr)),
            orgnr == null ? Arbeidskategori.ARBEIDSTAKER : Arbeidskategori.INAKTIV, true);

        return OpptjeningAktiviteter.nyPeriodeOrgnr(utledOpptjeningAktivitetType(relatertYtelseType), Periode.of(fom, tom), orgnr);
    }

    private OpptjeningAktivitetType utledOpptjeningAktivitetType(ArbeidType arbeidType) {
        return OpptjeningAktivitetType.hentFraArbeidTypeRelasjoner()
            .get(arbeidType).stream()
            .findFirst()
            .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private OpptjeningAktivitetType utledOpptjeningAktivitetType(FagsakYtelseType ytelseType) {
        return OpptjeningAktivitetType.hentFraFagsakYtelseTyper()
                .get(ytelseType).stream()
                .findFirst()
                .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private void leggTilAktørytelse(BehandlingReferanse behandlingReferanse, LocalDate fom, LocalDate tom, // NOSONAR - brukes bare til test
                                    RelatertYtelseTilstand relatertYtelseTilstand, String saksnummer, FagsakYtelseType ytelseType,
                                    List<YtelseStørrelse> ytelseStørrelseList, Arbeidskategori arbeidskategori, boolean medYtelseAnvist) {
        if (medYtelseAnvist) {
            iayTestUtil.leggTilAktørytelse(behandlingReferanse, fom, tom, relatertYtelseTilstand, saksnummer, ytelseType, ytelseStørrelseList, arbeidskategori,
                Periode.of(fom, tom));
        } else {
            iayTestUtil.leggTilAktørytelse(behandlingReferanse, fom, tom, relatertYtelseTilstand, saksnummer, ytelseType, ytelseStørrelseList, arbeidskategori);
        }
    }
}
