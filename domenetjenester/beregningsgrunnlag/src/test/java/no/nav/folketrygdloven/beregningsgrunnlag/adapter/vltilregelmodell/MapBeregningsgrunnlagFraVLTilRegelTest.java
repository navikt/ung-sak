package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell;

import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.MINUS_DAYS_10;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.MINUS_DAYS_5;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.MINUS_YEARS_1;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.MINUS_YEARS_2;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.NOW;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLBGAktivitetStatus;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLBGAktivitetStatusFL;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLBGPStatus;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLBGPStatusForSN;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLBGPeriode;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLBeregningsgrunnlag;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLSammenligningsgrunnlag;
import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.RegelMapperTestDataHelper.buildVLSammenligningsgrunnlagPrStatus;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektskilde.INNTEKTSKOMPONENTEN_BEREGNING;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektskilde.INNTEKTSKOMPONENTEN_SAMMENLIGNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.felles.BeregningUtils;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Inntektskilde;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Periodeinntekt;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.SammenligningsGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.beregningsgrunnlag.BeregningAktivitetTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.LagMapBeregningsgrunnlagFraVLTilRegel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel.Type;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelseBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.SammenligningsgrunnlagType;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.ArbeidType;
import no.nav.k9.kodeverk.iay.InntektsKilde;
import no.nav.k9.kodeverk.iay.Inntektskategori;
import no.nav.k9.kodeverk.iay.InntektspostType;
import no.nav.k9.kodeverk.iay.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.iay.TemaUnderkategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class MapBeregningsgrunnlagFraVLTilRegelTest {

    private static final int MELDEKORTSATS1 = 1000;
    private static final int MELDEKORTSATS2 = 1100;
    private static final int SIGRUN_2015 = 500000;
    private static final int SIGRUN_2016 = 600000;
    private static final int SIGRUN_2017 = 700000;
    private static final int TOTALINNTEKT_SIGRUN = SIGRUN_2015 + SIGRUN_2016 + SIGRUN_2017;

    private static final LocalDate FIRST_DAY_PREVIOUS_MONTH = LocalDate.now().minusMonths(1).withDayOfMonth(1);
    private static final Integer INNTEKT_BELOP = 25000;
    private static final LocalDate OPPRINNELIG_IDENTDATO = null;
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private BehandlingReferanse behandlingReferanse;
    private YrkesaktivitetBuilder yrkesaktivitetBuilder;
    private String virksomhetA = "42";
    private String virksomhetB = "47";

    private TestScenarioBuilder scenario;
    private InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseBuilder;
    private MapBeregningsgrunnlagFraVLTilRegel mapper;

    private Collection<Inntektsmelding> inntektsmeldinger = List.of();
    private FakeUnleash unleash = new FakeUnleash();

    @Before
    public void setup() {
        // Virksomhet A og B
        scenario = TestScenarioBuilder.nyttScenario();
        mapper = LagMapBeregningsgrunnlagFraVLTilRegel.lagMapper(repositoryProvider.getBeregningsgrunnlagRepository(), unleash);
    }

    private InntektArbeidYtelseAggregatBuilder opprettForBehandling(AbstractTestScenario<?> scenario) {
        LocalDate fraOgMed = MINUS_YEARS_1.withDayOfMonth(1);
        LocalDate tilOgMed = fraOgMed.plusYears(1);
        inntektArbeidYtelseBuilder = scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd();
        AktørId aktørId = scenario.getSøkerAktørId();
        lagAktørArbeid(inntektArbeidYtelseBuilder, aktørId, Arbeidsgiver.virksomhet(virksomhetA), fraOgMed, tilOgMed, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
            Optional.empty());
        for (LocalDate dt = fraOgMed; dt.isBefore(tilOgMed); dt = dt.plusMonths(1)) {
            lagInntekt(inntektArbeidYtelseBuilder, aktørId, virksomhetA, dt, dt.plusMonths(1));
        }
        return inntektArbeidYtelseBuilder;
    }

    private BehandlingReferanse lagBehandling(TestScenarioBuilder scenario) {
        opprettForBehandling(scenario);
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        return behandlingReferanse;
    }

    private BehandlingReferanse lagIAYforTilstøtendeYtelser(AbstractTestScenario<?> scenario, BeregningsgrunnlagEntitet beregningsgrunnlag) {
        LocalDate skjæring = beregningsgrunnlag.getSkjæringstidspunkt();
        InntektArbeidYtelseAggregatBuilder iayBuilder = opprettForBehandling(scenario);
        AktørYtelseBuilder aktørYtelseBuilder = iayBuilder.getAktørYtelseBuilder(scenario.getSøkerAktørId());
        YtelseBuilder ytelse = lagYtelse(skjæring.minusMonths(1).plusDays(1), skjæring.plusMonths(6),
            new BigDecimal(MELDEKORTSATS1),
            BeregningUtils.MAX_UTBETALING_PROSENT_AAP_DAG,
            skjæring.minusMonths(1).plusDays(2),
            skjæring.minusMonths(1).plusDays(16));
        aktørYtelseBuilder.leggTilYtelse(ytelse);
        ytelse = lagYtelse(skjæring.minusMonths(3), skjæring.minusMonths(1),
            new BigDecimal(MELDEKORTSATS2),
            new BigDecimal(100),
            skjæring.minusMonths(1).minusDays(13),
            skjæring.minusMonths(1).plusDays(1));
        aktørYtelseBuilder.leggTilYtelse(ytelse);
        iayBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        return behandlingReferanse;
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }

    private BehandlingReferanse lagIAYforTilstøtendeYtelserForMarginalTilfelle(AbstractTestScenario<?> scenario, BeregningsgrunnlagEntitet beregningsgrunnlag) {
        LocalDate skjæring = beregningsgrunnlag.getSkjæringstidspunkt();
        InntektArbeidYtelseAggregatBuilder iayBuilder = opprettForBehandling(scenario);
        AktørYtelseBuilder aktørYtelseBuilder = iayBuilder.getAktørYtelseBuilder(scenario.getSøkerAktørId());
        YtelseBuilder ytelse = lagYtelse(skjæring.minusWeeks(2), skjæring.plusMonths(6),
            new BigDecimal(MELDEKORTSATS1),
            BeregningUtils.MAX_UTBETALING_PROSENT_AAP_DAG.subtract(BigDecimal.TEN),
            skjæring.minusDays(5),
            skjæring.plusDays(9));
        aktørYtelseBuilder.leggTilYtelse(ytelse);
        iayBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        return behandlingReferanse;
    }

    private YtelseBuilder lagYtelse(LocalDate fom, LocalDate tom,
                                    BigDecimal beløp, BigDecimal utbetalingsgrad, LocalDate meldekortFom, LocalDate meldekortTom) {
        Saksnummer sakId = new Saksnummer("1200094");

        YtelseBuilder ytelselseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medSaksnummer(sakId);
        ytelselseBuilder.tilbakestillAnvisteYtelser();
        return ytelselseBuilder
            .medKilde(Fagsystem.ARENA)
            .medYtelseType(FagsakYtelseType.DAGPENGER)
            .medBehandlingsTema(TemaUnderkategori.UDEFINERT)
            .medStatus(RelatertYtelseTilstand.AVSLUTTET)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medSaksnummer(sakId)
            .medYtelseGrunnlag(
                ytelselseBuilder.getGrunnlagBuilder()
                    .medOpprinneligIdentdato(OPPRINNELIG_IDENTDATO)
                    .medVedtaksDagsats(beløp)
                    .medInntektsgrunnlagProsent(new BigDecimal(99.00))
                    .medDekningsgradProsent(new BigDecimal(98.00))
                    .medYtelseStørrelse(YtelseStørrelseBuilder.ny()
                        .medBeløp(new BigDecimal(100000.50))
                        .build())
                    .build())
            .medYtelseAnvist(ytelselseBuilder.getAnvistBuilder()
                .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(meldekortFom, meldekortTom))
                .medDagsats(beløp)
                .medUtbetalingsgradProsent(utbetalingsgrad)
                .build());
    }

    private AktørArbeid lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId,
                                       Arbeidsgiver arbeidsgiver, LocalDate fom, LocalDate tom, ArbeidType arbeidType,
                                       Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
            .getAktørArbeidBuilder(aktørId);

        Opptjeningsnøkkel opptjeningsnøkkel = arbeidsforholdRef.map(ref -> new Opptjeningsnøkkel(ref, arbeidsgiver))
            .orElseGet(() -> Opptjeningsnøkkel.forOrgnummer(arbeidsgiver.getIdentifikator()));
        yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel, arbeidType);
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(BigDecimal.valueOf(100));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtale)
            .medArbeidType(arbeidType)
            .medArbeidsgiver(arbeidsgiver);

        yrkesaktivitetBuilder.medArbeidsforholdId(arbeidsforholdRef.orElse(null));
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        return aktørArbeidBuilder.build();
    }

    private void lagInntekt(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
                            LocalDate fom, LocalDate tom) {
        Opptjeningsnøkkel opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);

        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);

        Stream.of(InntektsKilde.INNTEKT_BEREGNING, InntektsKilde.INNTEKT_SAMMENLIGNING).forEach(kilde -> {
            InntektBuilder inntektBuilder = aktørInntektBuilder.getInntektBuilder(kilde, opptjeningsnøkkel);
            InntektspostBuilder inntektspost = InntektspostBuilder.ny()
                .medBeløp(BigDecimal.valueOf(INNTEKT_BELOP))
                .medPeriode(fom, tom)
                .medInntektspostType(InntektspostType.LØNN);
            inntektBuilder.leggTilInntektspost(inntektspost).medArbeidsgiver(yrkesaktivitetBuilder.build().getArbeidsgiver());
            aktørInntektBuilder.leggTilInntekt(inntektBuilder);
            inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);
        });
    }

    @Test
    public void skalMapBGForSN() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario);
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        buildVLSammenligningsgrunnlag(beregningsgrunnlag);
        buildVLSammenligningsgrunnlagPrStatus(beregningsgrunnlag, SammenligningsgrunnlagType.SAMMENLIGNING_SN);
        buildVLBGAktivitetStatus(beregningsgrunnlag);
        leggTilInntekterFraSigrun(scenario.getSøkerAktørId());
        BeregningsgrunnlagPeriode bgPeriode = buildVLBGPeriode(beregningsgrunnlag);
        buildVLBGPStatusForSN(bgPeriode);

        // Act
        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagGrunnlag(beregningsgrunnlag);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref, grunnlag);

        // Assert
        assertThat(resultatBG).isNotNull();
        verifiserInntekterFraSigrun(resultatBG, TOTALINNTEKT_SIGRUN);
        assertThat(resultatBG.getSkjæringstidspunkt()).isEqualTo(MINUS_DAYS_5);
        assertThat(resultatBG.getSammenligningsGrunnlag().getRapportertPrÅr().doubleValue()).isEqualTo(1098318.12, within(0.01));
        assertThat(resultatBG.getSammenligningsGrunnlag().getAvvikPromille()).isEqualTo(220L);
        assertThat(resultatBG.getBeregningsgrunnlagPerioder()).hasSize(1);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode resultatBGP = resultatBG
            .getBeregningsgrunnlagPerioder().get(0);
        assertThat(resultatBGP.getBeregningsgrunnlagPeriode().getFom()).isEqualTo(resultatBG.getSkjæringstidspunkt());
        assertThat(resultatBGP.getBeregningsgrunnlagPeriode().getTom()).isEqualTo(resultatBG.getSkjæringstidspunkt().plusYears(3));
        assertThat(resultatBGP.getBruttoPrÅr().doubleValue()).isEqualTo(4444432.32, within(0.01));
        assertThat(resultatBGP.getBeregningsgrunnlagPrStatus()).hasSize(1);
        resultatBGP.getBeregningsgrunnlagPrStatus().forEach(resultatBGPS -> {
            assertThat(resultatBGPS.getAktivitetStatus()).isEqualTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.SN);
            assertThat(resultatBGPS.getBeregningsperiode().getFom()).isEqualTo(MINUS_DAYS_10);
            assertThat(resultatBGPS.getBeregningsperiode().getTom()).isEqualTo(MINUS_DAYS_5);
            assertThat(resultatBGPS.getArbeidsforhold()).isEmpty();
            assertThat(resultatBGPS.getBeregnetPrÅr().doubleValue()).isEqualTo(1000.01, within(0.01));
            assertThat(resultatBGPS.samletNaturalytelseBortfaltMinusTilkommetPrÅr()).isZero();
        });
        assertThat(resultatBG.getSammenligningsGrunnlagPrAktivitetstatus(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.SN))
            .isNotNull();
        assertThat(resultatBG.getSammenligningsGrunnlagPrAktivitetstatus(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.SN)
            .getRapportertPrÅr().doubleValue()).isEqualTo(1098318.12, within(0.01));
        assertThat(resultatBG.getSammenligningsGrunnlagPrAktivitetstatus(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.SN)
            .getAvvikPromille()).isEqualTo(220L);
    }

    private Beregningsgrunnlag map(BehandlingReferanse ref, BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()))
            .medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);
        return mapper.map(input, grunnlag);
    }

    private void leggTilInntekterFraSigrun(AktørId aktørId) {
        AktørInntektBuilder builder = inntektArbeidYtelseBuilder.getAktørInntektBuilder(aktørId);
        InntektBuilder inntektBuilder = builder.getInntektBuilder(InntektsKilde.SIGRUN, Opptjeningsnøkkel.forType(aktørId.toString(), Type.AKTØR_ID));
        inntektBuilder.leggTilInntektspost(opprettInntektspostForSigrun(2015, SIGRUN_2015));
        inntektBuilder.leggTilInntektspost(opprettInntektspostForSigrun(2016, SIGRUN_2016));
        inntektBuilder.leggTilInntektspost(opprettInntektspostForSigrun(2017, SIGRUN_2017));
        builder.leggTilInntekt(inntektBuilder);
    }

    private InntektspostBuilder opprettInntektspostForSigrun(int år, int inntekt) {
        return InntektspostBuilder.ny()
            .medBeløp(BigDecimal.valueOf(inntekt))
            .medPeriode(LocalDate.of(år, Month.JANUARY, 1), LocalDate.of(år, Month.DECEMBER, 31))
            .medInntektspostType(InntektspostType.LØNN);
    }

    private void verifiserInntekterFraSigrun(final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG,
                                             int totalinntektSigrun) {
        assertThat(resultatBG.getInntektsgrunnlag()).isNotNull();
        Inntektsgrunnlag ig = resultatBG.getInntektsgrunnlag();
        List<Periodeinntekt> fraSigrun = ig.getPeriodeinntekter().stream().filter(mi -> mi.getInntektskilde().equals(Inntektskilde.SIGRUN))
            .collect(Collectors.toList());
        assertThat(fraSigrun).isNotEmpty();
        int total = fraSigrun.stream().map(Periodeinntekt::getInntekt).mapToInt(BigDecimal::intValue).sum();
        assertThat(total).isEqualTo(totalinntektSigrun);
    }

    @Test
    public void skalMapBGForArebidstakerMedFlereBGPStatuser() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario);
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatus(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildVLBGPeriode(beregningsgrunnlag);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, MINUS_YEARS_2, MINUS_YEARS_1,
            Arbeidsgiver.virksomhet(virksomhetA), OpptjeningAktivitetType.ARBEID);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, MINUS_YEARS_1, NOW, Arbeidsgiver.virksomhet(virksomhetB),
            OpptjeningAktivitetType.ARBEID);

        // Act
        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref,
            lagGrunnlag(beregningsgrunnlag));

        // Assert
        assertThat(resultatBG.getBeregningsgrunnlagPerioder()).hasSize(1);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode resultatBGP = resultatBG
            .getBeregningsgrunnlagPerioder().get(0);
        assertThat(resultatBGP.getBeregningsgrunnlagPrStatus()).hasSize(1);
        resultatBGP.getBeregningsgrunnlagPrStatus().forEach(resultatBGPS -> {
            assertThat(resultatBGPS.getAktivitetStatus()).isEqualTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.ATFL);
            assertThat(resultatBGPS.getBeregningsperiode()).isNull();
            assertThat(resultatBGPS.getBeregnetPrÅr().doubleValue()).isEqualTo(2000.02, within(0.01));
            assertThat(resultatBGPS.samletNaturalytelseBortfaltMinusTilkommetPrÅr().doubleValue()).isEqualTo(6464.64, within(0.01));
            assertThat(resultatBGPS.getArbeidsforhold()).hasSize(2);
            assertThat(resultatBGPS.getArbeidsforhold().get(0).getArbeidsgiverId()).isEqualTo("42");
            assertArbeidforhold(resultatBGPS.getArbeidsforhold().get(0), MINUS_YEARS_2, MINUS_YEARS_1);
            assertThat(resultatBGPS.getArbeidsforhold().get(1).getArbeidsgiverId()).isEqualTo("47");
            assertArbeidforhold(resultatBGPS.getArbeidsforhold().get(1), MINUS_YEARS_1, NOW);
        });
    }

    @Test
    public void skal_mappe_bg_for_arbeidstaker_hos_privatperson_og_virksomhet() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        behandlingReferanse = lagBehandling(scenario);
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatus(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildVLBGPeriode(beregningsgrunnlag);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, MINUS_YEARS_1, NOW, Arbeidsgiver.person(aktørId),
            OpptjeningAktivitetType.ARBEID);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, MINUS_YEARS_2, MINUS_YEARS_1,
            Arbeidsgiver.virksomhet(virksomhetB), OpptjeningAktivitetType.ARBEID);

        // Act
        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref,
            lagGrunnlag(beregningsgrunnlag));

        // Assert
        assertThat(resultatBG.getBeregningsgrunnlagPerioder()).hasSize(1);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode resultatBGP = resultatBG
            .getBeregningsgrunnlagPerioder().get(0);
        assertThat(resultatBGP.getBeregningsgrunnlagPrStatus()).hasSize(1);
        resultatBGP.getBeregningsgrunnlagPrStatus().forEach(resultatBGPS -> {
            assertThat(resultatBGPS.getAktivitetStatus()).isEqualTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.ATFL);
            assertThat(resultatBGPS.getBeregningsperiode()).isNull();
            assertThat(resultatBGPS.getBeregnetPrÅr().doubleValue()).isEqualTo(2000.02, within(0.01));
            assertThat(resultatBGPS.samletNaturalytelseBortfaltMinusTilkommetPrÅr().doubleValue()).isEqualTo(6464.64, within(0.01));
            assertThat(resultatBGPS.getArbeidsforhold()).hasSize(2);
            assertThat(resultatBGPS.getArbeidsforhold().get(0).getArbeidsforhold().getAktørId()).isEqualTo(aktørId.getId());
            assertArbeidforhold(resultatBGPS.getArbeidsforhold().get(0), MINUS_YEARS_1, NOW);
            assertThat(resultatBGPS.getArbeidsforhold().get(1).getArbeidsgiverId()).isEqualTo("47");
            assertArbeidforhold(resultatBGPS.getArbeidsforhold().get(1), MINUS_YEARS_2, MINUS_YEARS_1);
        });
    }

    @Test
    public void skalMapBGForATogSNBeregeningGPStatuser() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario);
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatus(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildVLBGPeriode(beregningsgrunnlag);
        buildVLBGPStatusForSN(bgPeriode);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, MINUS_YEARS_1, NOW, Arbeidsgiver.virksomhet(virksomhetA),
            OpptjeningAktivitetType.ARBEID);

        // Act
        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref,
            lagGrunnlag(beregningsgrunnlag));
        // Assert
        assertThat(resultatBG.getBeregningsgrunnlagPerioder()).hasSize(1);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode resultatBGP = resultatBG
            .getBeregningsgrunnlagPerioder().get(0);
        assertThat(resultatBGP.getBeregningsgrunnlagPrStatus()).hasSize(2);
        resultatBGP.getBeregningsgrunnlagPrStatus().forEach(resultatBGPStatus -> {
            if (resultatBGPStatus.getAktivitetStatus().equals(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.SN)) {
                assertThat(resultatBGPStatus.getArbeidsforhold()).isEmpty();
            } else {
                assertThat(resultatBGPStatus.getAktivitetStatus())
                    .isEqualTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.ATFL);
                assertThat(resultatBGPStatus.getArbeidsforhold()).hasSize(1);
            }
        });
    }

    @Test
    public void skalMapBGForArbeidstakerMedInntektsgrunnlag() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario);
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatus(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildVLBGPeriode(beregningsgrunnlag);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, MINUS_YEARS_1, NOW, Arbeidsgiver.virksomhet(virksomhetA),
            OpptjeningAktivitetType.ARBEID);

        // Act
        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref,
            lagGrunnlag(beregningsgrunnlag));

        // Assert
        assertThat(resultatBG.getBeregningsgrunnlagPerioder()).hasSize(1);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode resultatBGP = resultatBG
            .getBeregningsgrunnlagPerioder().get(0);

        List<Periodeinntekt> månedsinntekter = resultatBG.getInntektsgrunnlag().getPeriodeinntekter();
        assertThat(antallMånedsinntekter(månedsinntekter, INNTEKTSKOMPONENTEN_BEREGNING)).isEqualTo(12);
        assertThat(antallMånedsinntekter(månedsinntekter, INNTEKTSKOMPONENTEN_SAMMENLIGNING)).isEqualTo(12);
        assertThat(månedsinntekter).hasSize(24);
        Optional<Periodeinntekt> inntektBeregning = resultatBGP.getInntektsgrunnlag().getPeriodeinntekt(INNTEKTSKOMPONENTEN_BEREGNING,
            FIRST_DAY_PREVIOUS_MONTH);
        Optional<Periodeinntekt> inntektSammenligning = resultatBGP.getInntektsgrunnlag().getPeriodeinntekt(INNTEKTSKOMPONENTEN_SAMMENLIGNING,
            FIRST_DAY_PREVIOUS_MONTH);
        assertInntektsgrunnlag(inntektBeregning);
        assertInntektsgrunnlag(inntektSammenligning);
    }

    @Test
    public void skalMappeTilstøtendeYtelserDPogAAP() {
        // Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        behandlingReferanse = lagIAYforTilstøtendeYtelser(scenario, beregningsgrunnlag);
        buildVLBGAktivitetStatus(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildVLBGPeriode(beregningsgrunnlag);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.DAGPENGER, Inntektskategori.DAGPENGER, MINUS_DAYS_10, NOW);

        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref,
            lagGrunnlag(beregningsgrunnlag));

        List<Periodeinntekt> dpMånedsInntekter = resultatBG.getInntektsgrunnlag().getPeriodeinntekter().stream()
            .filter(mi -> mi.getInntektskilde().equals(Inntektskilde.TILSTØTENDE_YTELSE_DP_AAP))
            .collect(Collectors.toList());
        assertThat(dpMånedsInntekter).hasSize(1);
        BigDecimal dagsats = BigDecimal.valueOf(MELDEKORTSATS1);
        assertThat(dpMånedsInntekter.get(0).getInntekt()).isEqualByComparingTo(dagsats);
        assertThat(dpMånedsInntekter.get(0).getUtbetalingsgrad())
            .hasValueSatisfying(utbg -> assertThat(utbg).isEqualByComparingTo(BeregningUtils.MAX_UTBETALING_PROSENT_AAP_DAG));
    }

    @Test
    public void skalMappeTilstøtendeYtelserDPogAAPMarginalTilfelle() {
        // Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        behandlingReferanse = lagIAYforTilstøtendeYtelserForMarginalTilfelle(scenario, beregningsgrunnlag);
        buildVLBGAktivitetStatus(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildVLBGPeriode(beregningsgrunnlag);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.DAGPENGER, Inntektskategori.DAGPENGER, MINUS_DAYS_10, NOW);

        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref,
            lagGrunnlag(beregningsgrunnlag));

        List<Periodeinntekt> dpMånedsInntekter = resultatBG.getInntektsgrunnlag().getPeriodeinntekter().stream()
            .filter(mi -> mi.getInntektskilde().equals(Inntektskilde.TILSTØTENDE_YTELSE_DP_AAP))
            .collect(Collectors.toList());
        assertThat(dpMånedsInntekter).hasSize(1);
        BigDecimal dagsats = BigDecimal.valueOf(MELDEKORTSATS1);
        assertThat(dpMånedsInntekter.get(0).getInntekt()).isEqualByComparingTo(dagsats);
        assertThat(dpMånedsInntekter.get(0).getUtbetalingsgrad())
            .hasValueSatisfying(utbg -> assertThat(utbg).isEqualByComparingTo(BeregningUtils.MAX_UTBETALING_PROSENT_AAP_DAG));
    }

    @Test
    public void skalIkkeLageSammenligningsgrunnlagForArbeidstakerNårIkkeFinnesFraFør() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario);
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatus(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildVLBGPeriode(beregningsgrunnlag);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, MINUS_YEARS_1, NOW, Arbeidsgiver.virksomhet(virksomhetA),
            OpptjeningAktivitetType.ARBEID);
        // Act
        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref,
            lagGrunnlag(beregningsgrunnlag));
        // Assert
        assertThat(resultatBG.getSammenligningsGrunnlag()).isNull();
    }

    private BehandlingReferanse lagRefMedSkjæringstidspunkt(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(
            Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .build());
    }

    @Test
    public void skalLageSammenligningsgrunnlagForTilbakehopp() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario);
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatus(beregningsgrunnlag);
        buildVLBGPeriode(beregningsgrunnlag);
        buildVLSammenligningsgrunnlag(beregningsgrunnlag);
        repositoryProvider.getBeregningsgrunnlagRepository().lagre(behandlingReferanse.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT);

        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);

        // Act
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref,
            lagGrunnlag(beregningsgrunnlag));

        // Assert
        SammenligningsGrunnlag resultatSG = resultatBG.getSammenligningsGrunnlag();
        assertThat(resultatSG).isNotNull();
        Sammenligningsgrunnlag forrigeSG = beregningsgrunnlag.getSammenligningsgrunnlag();
        assertThat(resultatSG.getSammenligningsperiode())
            .isEqualTo(Periode.of(forrigeSG.getSammenligningsperiodeFom(), forrigeSG.getSammenligningsperiodeTom()));
        assertThat(resultatSG.getRapportertPrÅr()).isEqualByComparingTo(forrigeSG.getRapportertPrÅr());
        assertThat(resultatSG.getAvvikPromille()).isEqualTo(forrigeSG.getAvvikPromille());
    }

    @Test
    public void skalIkkeLageSammenligningsgrunnlagNårHarInnhentetNyeData() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario);
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatus(beregningsgrunnlag);
        buildVLBGPeriode(beregningsgrunnlag);
        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);

        // Act
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref,
            lagGrunnlag(beregningsgrunnlag));

        // Assert
        assertThat(resultatBG.getSammenligningsGrunnlag()).isNull();
    }

    @Test
    public void skalMappeTilRegelNårBrukerErFrilanser() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario);
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatusFL(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildVLBGPeriode(beregningsgrunnlag);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER, MINUS_YEARS_2, MINUS_YEARS_1, null, OpptjeningAktivitetType.FRILANS);
        buildVLSammenligningsgrunnlagPrStatus(beregningsgrunnlag, SammenligningsgrunnlagType.SAMMENLIGNING_FL);
        // Act
        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref,
            lagGrunnlag(beregningsgrunnlag));
        // Assert
        assertThat(resultatBG.getBeregningsgrunnlagPerioder()).hasSize(1);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode resultatBGP = resultatBG
            .getBeregningsgrunnlagPerioder().get(0);
        assertThat(resultatBGP.getBeregningsgrunnlagPrStatus()).hasSize(1);
        resultatBGP.getBeregningsgrunnlagPrStatus().forEach(resultatBGPS -> {
            assertThat(resultatBGPS.getBeregningsperiode()).isNull();
            assertThat(resultatBGPS.getBeregnetPrÅr().doubleValue()).isEqualTo(1000.01, within(0.01));
        });
        assertThat(resultatBG.getSammenligningsGrunnlagPrAktivitetstatus(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.FL))
            .isNotNull();
        assertThat(resultatBG.getSammenligningsGrunnlagPrAktivitetstatus(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.FL)
            .getRapportertPrÅr().doubleValue()).isEqualTo(1098318.12, within(0.01));
        assertThat(resultatBG.getSammenligningsGrunnlagPrAktivitetstatus(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.FL)
            .getAvvikPromille()).isEqualTo(220L);
    }

    @Test
    public void skalMappeTilRegelNårBrukerErArbeidstaker() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario);
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildVLBeregningsgrunnlag();
        buildVLBGAktivitetStatus(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildVLBGPeriode(beregningsgrunnlag);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, MINUS_YEARS_2, MINUS_YEARS_1,
            Arbeidsgiver.virksomhet(virksomhetA), OpptjeningAktivitetType.ARBEID);
        buildVLBGPStatus(bgPeriode, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, MINUS_YEARS_1, NOW, Arbeidsgiver.virksomhet(virksomhetB),
            OpptjeningAktivitetType.ARBEID);
        buildVLSammenligningsgrunnlagPrStatus(beregningsgrunnlag, SammenligningsgrunnlagType.SAMMENLIGNING_AT);
        // Act
        BehandlingReferanse ref = lagRefMedSkjæringstidspunkt(behandlingReferanse);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag resultatBG = map(ref,
            lagGrunnlag(beregningsgrunnlag));
        // Assert
        assertThat(resultatBG.getBeregningsgrunnlagPerioder()).hasSize(1);
        final no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode resultatBGP = resultatBG
            .getBeregningsgrunnlagPerioder().get(0);
        assertThat(resultatBGP.getBeregningsgrunnlagPrStatus()).hasSize(1);
        resultatBGP.getBeregningsgrunnlagPrStatus().forEach(resultatBGPS -> {
            assertThat(resultatBGPS.getAktivitetStatus()).isEqualTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.ATFL);
            assertThat(resultatBGPS.getBeregningsperiode()).isNull();
            assertThat(resultatBGPS.getBeregnetPrÅr().doubleValue()).isEqualTo(2000.02, within(0.01));
            assertThat(resultatBGPS.samletNaturalytelseBortfaltMinusTilkommetPrÅr().doubleValue()).isEqualTo(6464.64, within(0.01));
            assertThat(resultatBGPS.getArbeidsforhold()).hasSize(2);
            assertThat(resultatBGPS.getArbeidsforhold().get(0).getArbeidsgiverId()).isEqualTo("42");
            assertArbeidforhold(resultatBGPS.getArbeidsforhold().get(0), MINUS_YEARS_2, MINUS_YEARS_1);
            assertThat(resultatBGPS.getArbeidsforhold().get(1).getArbeidsgiverId()).isEqualTo("47");
            assertArbeidforhold(resultatBGPS.getArbeidsforhold().get(1), MINUS_YEARS_1, NOW);
        });
        assertThat(resultatBG.getSammenligningsGrunnlagPrAktivitetstatus(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.AT))
            .isNotNull();
        assertThat(resultatBG.getSammenligningsGrunnlagPrAktivitetstatus(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.AT)
            .getRapportertPrÅr().doubleValue()).isEqualTo(1098318.12, within(0.01));
        assertThat(resultatBG.getSammenligningsGrunnlagPrAktivitetstatus(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatus.AT)
            .getAvvikPromille()).isEqualTo(220L);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagGrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medRegisterAktiviteter(BeregningAktivitetTestUtil.opprettBeregningAktiviteter(SKJÆRINGSTIDSPUNKT, OpptjeningAktivitetType.ARBEID))
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    private long antallMånedsinntekter(List<Periodeinntekt> månedsinntekter, Inntektskilde inntektskomponentenBeregning) {
        return månedsinntekter.stream().filter(m -> m.getInntektskilde().equals(inntektskomponentenBeregning)).count();
    }

    private void assertInntektsgrunnlag(Optional<Periodeinntekt> inntektBeregning) {
        assertThat(inntektBeregning).isPresent();
        assertThat(inntektBeregning).hasValueSatisfying(månedsinntekt -> {
            assertThat(månedsinntekt.getInntekt().intValue()).isEqualTo(INNTEKT_BELOP);
            assertThat(månedsinntekt.getFom()).isEqualTo(FIRST_DAY_PREVIOUS_MONTH);
            assertThat(månedsinntekt.fraInntektsmelding()).isFalse();
        });
    }

    private void assertArbeidforhold(BeregningsgrunnlagPrArbeidsforhold arbeidsforhold, LocalDate fom, LocalDate tom) {
        assertThat(arbeidsforhold.getBeregnetPrÅr().doubleValue()).isEqualTo(1000.01, within(0.01));
        assertThat(arbeidsforhold.getNaturalytelseBortfaltPrÅr())
            .hasValueSatisfying(naturalYtelseBortfalt -> assertThat(naturalYtelseBortfalt.doubleValue()).isEqualTo(3232.32, within(0.01)));
        assertThat(arbeidsforhold.getBeregningsperiode()).isEqualTo(Periode.of(fom, tom));
    }
}
