package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBeregningsgrunnlagFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Sammenligningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.SammenligningsgrunnlagPrStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.beregningsgrunnlag.BeregningAktivitetTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.LagMapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.verdikjede.VerdikjedeTestHjelper;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAksjonspunktDefinisjon;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak;
import no.nav.k9.kodeverk.beregningsgrunnlag.SammenligningsgrunnlagType;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.ArbeidType;
import no.nav.k9.kodeverk.iay.Inntektskategori;
import no.nav.k9.kodeverk.iay.NaturalYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.vedtak.konfig.Tid;

public class ForeslåBeregningsgrunnlagTest {

    private static final String ORGNR = "987123987";
    private static final double MÅNEDSINNTEKT1 = 12345d;
    private static final double MÅNEDSINNTEKT2 = 6000d;
    private static final double ÅRSINNTEKT1 = MÅNEDSINNTEKT1 * 12;
    private static final double ÅRSINNTEKT2 = MÅNEDSINNTEKT2 * 12;
    private static final double NATURALYTELSE_I_PERIODE_2 = 200d;
    private static final double NATURALYTELSE_I_PERIODE_3 = 400d;
    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.APRIL, 10);
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = SKJÆRINGSTIDSPUNKT_OPPTJENING;
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000);
    private static final String ARBEIDSFORHOLD_ORGNR1 = "654";
    private static final String ARBEIDSFORHOLD_ORGNR2 = "765";
    private static final LocalDate MINUS_YEARS_2 = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(2);
    private static final LocalDate MINUS_YEARS_1 = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1);
    private static final LocalDate ARBEIDSPERIODE_FOM = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1);
    private static final LocalDate ARBEIDSPERIODE_TOM = SKJÆRINGSTIDSPUNKT_OPPTJENING.plusYears(2);

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private ForeslåBeregningsgrunnlag tjeneste;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private BehandlingReferanse behandlingReferanse;
    private TestScenarioBuilder scenario;
    private AktørId beregningsAkrød1 = AktørId.dummy();
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    private VerdikjedeTestHjelper verdikjedeTestHjelper = new VerdikjedeTestHjelper();
    private FakeUnleash unleash = new FakeUnleash();
    private static final String TOGGLE_SPLITTE_SAMMENLIGNING = "fpsak.splitteSammenligningATFL";

    @Before
    public void setup() {
        MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel = LagMapBeregningsgrunnlagFraVLTilRegel.lagMapper(repositoryProvider.getBeregningsgrunnlagRepository(), unleash);
        MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel = new MapBeregningsgrunnlagFraRegelTilVL();
        scenario = TestScenarioBuilder.nyttScenario();
        AksjonspunktUtlederForeslåBeregning aksjonspunktUtleder = new AksjonspunktUtlederForeslåBeregning();
        tjeneste = new ForeslåBeregningsgrunnlag(oversetterTilRegel, oversetterFraRegel, aksjonspunktUtleder, unleash);
        beregningsgrunnlag = lagBeregningsgrunnlagAT(true);
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlagAT(boolean erArbeidsgiverVirksomhet) {
        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.builder();
        beregningsgrunnlagBuilder.medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .medGrunnbeløp(GRUNNBELØP);
        beregningsgrunnlagBuilder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER));
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, null)
            .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(lagBgAndelArbeidsforhold(ARBEIDSPERIODE_FOM, ARBEIDSPERIODE_TOM,
                    erArbeidsgiverVirksomhet ? Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1) : Arbeidsgiver.person(beregningsAkrød1)))
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medBeregningsperiode(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(3).withDayOfMonth(1),
                    SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1))));
        return beregningsgrunnlagBuilder.build();
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlagATFL_SN(boolean nyIArbeidslivet) {
        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.builder();
        beregningsgrunnlagBuilder.medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .medGrunnbeløp(GRUNNBELØP);
        beregningsgrunnlagBuilder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.KOMBINERT_AT_SN));
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, null)
            .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(lagBgAndelArbeidsforhold(ARBEIDSPERIODE_FOM, ARBEIDSPERIODE_TOM, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1)))
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medBeregningsperiode(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(3).withDayOfMonth(1),
                    SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1)))
            .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
                .medNyIArbeidslivet(nyIArbeidslivet)
                .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                    .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                    .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                    .medArbeidsperiodeTom(LocalDate.now().plusYears(2)))));
        return beregningsgrunnlagBuilder.build();
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlagFL() {
        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.builder();
        beregningsgrunnlagBuilder.medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
            .medGrunnbeløp(GRUNNBELØP);
        beregningsgrunnlagBuilder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.FRILANSER));
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT_BEREGNING, null)
            .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(lagBgAndelArbeidsforhold(ARBEIDSPERIODE_FOM, ARBEIDSPERIODE_TOM, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1)))
                .medAktivitetStatus(AktivitetStatus.FRILANSER)
                .medInntektskategori(Inntektskategori.FRILANSER)
                .medBeregningsperiode(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(3).withDayOfMonth(1),
                    SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1))));
        return beregningsgrunnlagBuilder.build();
    }

    private BGAndelArbeidsforhold.Builder lagBgAndelArbeidsforhold(LocalDate fom, LocalDate tom, Arbeidsgiver arbeidsgiver) {
        return BGAndelArbeidsforhold.builder().medArbeidsperiodeFom(fom).medArbeidsperiodeTom(tom).medArbeidsgiver(arbeidsgiver);
    }

    private BehandlingReferanse lagBehandling(TestScenarioBuilder scenario,
                                     BigDecimal inntektSammenligningsgrunnlag,
                                     BigDecimal inntektBeregningsgrunnlag, Arbeidsgiver arbeidsgiver, LocalDate fraOgMed, LocalDate tilOgMed) {

        var inntektArbeidYtelseBuilder = scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd();

        AktørId aktørId = scenario.getSøkerAktørId();
        verdikjedeTestHjelper.lagAktørArbeid(inntektArbeidYtelseBuilder, aktørId, arbeidsgiver, fraOgMed, tilOgMed, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        verdikjedeTestHjelper.lagAktørArbeid(inntektArbeidYtelseBuilder, aktørId, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR2), fraOgMed, tilOgMed,
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        for (LocalDate dt = fraOgMed; dt.isBefore(tilOgMed); dt = dt.plusMonths(1)) {
            verdikjedeTestHjelper.lagInntektForSammenligning(inntektArbeidYtelseBuilder, aktørId, dt, dt.plusMonths(1), inntektSammenligningsgrunnlag,
                arbeidsgiver);
            verdikjedeTestHjelper.lagInntektForArbeidsforhold(inntektArbeidYtelseBuilder, aktørId, dt, dt.plusMonths(1), inntektBeregningsgrunnlag,
                arbeidsgiver);
        }

        BehandlingReferanse behandlingReferanse = lagre(scenario);
        return behandlingReferanse;
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }

    private BehandlingReferanse lagBehandlingFL(TestScenarioBuilder scenario,
                                       BigDecimal inntektSammenligningsgrunnlag,
                                       BigDecimal inntektFrilans, String virksomhetOrgnr) {
        LocalDate fraOgMed = MINUS_YEARS_1.withDayOfMonth(1);
        LocalDate tilOgMed = fraOgMed.plusYears(1);
        verdikjedeTestHjelper.initBehandlingFL(scenario, inntektSammenligningsgrunnlag, inntektFrilans, virksomhetOrgnr, fraOgMed, tilOgMed);
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        return behandlingReferanse;
    }

    private void lagKortvarigArbeidsforhold(BeregningsgrunnlagEntitet beregningsgrunnlag, LocalDate fomDato, LocalDate tomDato) {
        BeregningsgrunnlagPrStatusOgAndel andel = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, tomDato));
        Yrkesaktivitet yrkesaktivitet = YrkesaktivitetBuilder
            .oppdatere(Optional.empty()).leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsforholdId(andel.getBgAndelArbeidsforhold().get().getArbeidsforholdRef())
            .medArbeidsgiver(andel.getArbeidsgiver().get()).build();
        Optional<InntektArbeidYtelseAggregat> registerVersjon = iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()).getRegisterVersjon();
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(registerVersjon, VersjonType.REGISTER);

        Optional<AktørArbeid> aktørArbeid = registerVersjon.stream()
        .flatMap(iay -> iay.getAktørArbeid().stream())
        .filter(a -> a.getAktørId().equals(behandlingReferanse.getAktørId())).findFirst();
        Yrkesaktivitet ya = aktørArbeid.get().hentAlleYrkesaktiviteter()
        .stream()
        .filter(y -> y.equals(yrkesaktivitet))
        .findFirst().get();

        builder.getAktørArbeidBuilder(behandlingReferanse.getAktørId())
            .getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(yrkesaktivitet), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .getAktivitetsAvtaleBuilder(ya.getAlleAktivitetsAvtaler().iterator().next().getPeriode(), true)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, tomDato));

        iayTjeneste.lagreIayAggregat(behandlingReferanse.getBehandlingId(), builder);
    }

    @Test
    public void skalLageEnPeriode() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_1.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), BigDecimal.valueOf(MÅNEDSINNTEKT1 + 1000), null, null);
        var inntektsmeldinger = List.of(im1);
        // Act
        BeregningsgrunnlagRegelResultat resultat = act(beregningsgrunnlag, inntektsmeldinger);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(1);
        verifiserSammenligningsgrunnlag(resultat.getBeregningsgrunnlag().getSammenligningsgrunnlag(), ÅRSINNTEKT1,
            SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1), SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1),
            81L);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), (MÅNEDSINNTEKT1 + 1000) * 12,
            null, null);
    }

    @Test
    public void skalLageEnPeriodeNårNaturalytelseBortfallerPåSkjæringstidspunktet() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_1.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        splitBeregningsgrunnlagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT_BEREGNING, PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        var im1 = opprettInntektsmeldingNaturalytelseBortfaller(Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            BigDecimal.valueOf(NATURALYTELSE_I_PERIODE_2), SKJÆRINGSTIDSPUNKT_BEREGNING);
        var inntektsmeldinger = List.of(im1);
        // Act
        BeregningsgrunnlagRegelResultat resultat = act(beregningsgrunnlag, inntektsmeldinger);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1, PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1,
            NATURALYTELSE_I_PERIODE_2 * 12, null);
    }

    private BeregningsgrunnlagRegelResultat act(BeregningsgrunnlagEntitet beregningsgrunnlag, Collection<Inntektsmelding> inntektsmeldinger) {
        InntektArbeidYtelseGrunnlag gr = iayTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(Optional.of(gr)).medInntektsmeldinger(inntektsmeldinger).build();
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        var input = new BeregningsgrunnlagInput(lagReferanseMedStp(behandlingReferanse), iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, foreldrepengerGrunnlag);
        return act(input, beregningsgrunnlag);
    }

    @Test
    public void skalLageToPerioderNaturalYtelseBortfaller() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_1.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        splitBeregningsgrunnlagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4), PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        var im1 = opprettInntektsmeldingNaturalytelseBortfaller(Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            BigDecimal.valueOf(NATURALYTELSE_I_PERIODE_2), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(4));
        var inntektsmeldinger = List.of(im1);
        // Act
        BeregningsgrunnlagRegelResultat resultat = act(beregningsgrunnlag, inntektsmeldinger);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(2);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4).minusDays(1), 1);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null, null);
        periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(1);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4), null, 1, PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1,
            NATURALYTELSE_I_PERIODE_2 * 12, null);
    }

    @Test
    public void skalLageToPerioderNaturalYtelseTilkommer() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_1.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        splitBeregningsgrunnlagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4), PeriodeÅrsak.NATURALYTELSE_TILKOMMER);
        var im1 = opprettInntektsmeldingNaturalytelseTilkommer(Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            BigDecimal.valueOf(NATURALYTELSE_I_PERIODE_2), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(4));
        var inntektsmeldinger = List.of(im1);

        // Act
        BeregningsgrunnlagRegelResultat resultat = act(beregningsgrunnlag, inntektsmeldinger);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(2);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4).minusDays(1), 1);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null, null);
        periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(1);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4), null, 1, PeriodeÅrsak.NATURALYTELSE_TILKOMMER);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null,
            NATURALYTELSE_I_PERIODE_2 * 12);
    }

    @Test
    public void skalLageToPerioderKortvarigArbeidsforhold() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_1.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        BeregningsgrunnlagEntitet nyttGrunnlag = beregningsgrunnlag;
        BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel = nyttGrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()
            .get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(eksisterendeAndel)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder(eksisterendeAndel.getBgAndelArbeidsforhold().orElse(null))
                .medTidsbegrensetArbeidsforhold(true))
            .build(nyttGrunnlag.getBeregningsgrunnlagPerioder().get(0));
        lagKortvarigArbeidsforhold(nyttGrunnlag, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(4).minusDays(1));
        Collection<Inntektsmelding> inntektsmeldinger = List.of();

        // Før steget er det ingen inntekter på andelen
        // Act
        BeregningsgrunnlagRegelResultat resultat = act(beregningsgrunnlag, inntektsmeldinger);
        // Her skulle det vært inntekter på andelen
        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(2);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4).minusDays(1), 1);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null, null);
        periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(1);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4), null, 1, PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null, null);
    }

    @Test
    public void skalLageTrePerioderKortvarigArbeidsforholdOgNaturalYtelse() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_1.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        BeregningsgrunnlagEntitet nyttGrunnlag = beregningsgrunnlag;
        BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel = nyttGrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()
            .get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(eksisterendeAndel)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder(eksisterendeAndel
                .getBgAndelArbeidsforhold().orElse(null)).medTidsbegrensetArbeidsforhold(true))
            .build(nyttGrunnlag.getBeregningsgrunnlagPerioder().get(0));
        var im1 = opprettInntektsmeldingNaturalytelseBortfaller(Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            BigDecimal.valueOf(NATURALYTELSE_I_PERIODE_2), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(3));
        splitBeregningsgrunnlagPeriode(nyttGrunnlag, SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(3), PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        lagKortvarigArbeidsforhold(nyttGrunnlag, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(4).minusDays(1));
        var inntektsmeldinger = List.of(im1);

        // Act
        BeregningsgrunnlagRegelResultat resultat = act(beregningsgrunnlag, inntektsmeldinger);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(3);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(3).minusDays(1), 1);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null, null);

        periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(1);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(3), SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4).minusDays(1), 1,
            PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1,
            NATURALYTELSE_I_PERIODE_2 * 12, null);

        periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(2);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4), null, 1, PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1,
            NATURALYTELSE_I_PERIODE_2 * 12, null);
    }

    @Test
    public void skalLageEnPeriodeFrilanser() {
        // Arrange
        BeregningsgrunnlagEntitet grunnlagFL = lagBeregningsgrunnlagFL();
        BeregningsgrunnlagPrStatusOgAndel.builder(grunnlagFL.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0))
            .build(grunnlagFL.getBeregningsgrunnlagPerioder().get(0));
        behandlingReferanse = lagBehandlingFL(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1), ARBEIDSFORHOLD_ORGNR1);
        Collection<Inntektsmelding> inntektsmeldinger = List.of();

        // Act
        BeregningsgrunnlagRegelResultat resultat = act(grunnlagFL, inntektsmeldinger);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verifiserBGFL(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1);
    }

    @Test
    public void skal_lage_en_periode_for_private_arbeidsgiver() {
        // Arrange
        Arbeidsgiver privateArbeidsgiver = Arbeidsgiver.person(beregningsAkrød1);
        BeregningsgrunnlagEntitet grunnlagAT = lagBeregningsgrunnlagAT(false);
        BeregningsgrunnlagPrStatusOgAndel.builder(grunnlagAT.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0))
            .build(grunnlagAT.getBeregningsgrunnlagPerioder().get(0));
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1), privateArbeidsgiver,
            MINUS_YEARS_1.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        Collection<Inntektsmelding> inntektsmeldinger = List.of();

        // Act
        BeregningsgrunnlagRegelResultat resultat = act(grunnlagAT, inntektsmeldinger);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), privateArbeidsgiver, ÅRSINNTEKT1, null, null);
    }

    private void splitBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag, LocalDate nyPeriodeFom, PeriodeÅrsak nyPeriodeÅrsak) {
        List<BeregningsgrunnlagPeriode> perioder = beregningsgrunnlag.getBeregningsgrunnlagPerioder();
        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode = perioder.get(perioder.size() - 1);
        if (beregningsgrunnlagPeriode.getBeregningsgrunnlagPeriodeFom().equals(nyPeriodeFom)) {
            BeregningsgrunnlagPeriode.builder(beregningsgrunnlagPeriode)
                .leggTilPeriodeÅrsak(nyPeriodeÅrsak);
            return;
        }
        BeregningsgrunnlagPeriode.builder(beregningsgrunnlagPeriode)
            .medBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode.getBeregningsgrunnlagPeriodeFom(), nyPeriodeFom.minusDays(1));

        BeregningsgrunnlagPeriode nyBeregningsgrunnlagPeriode = Kopimaskin.deepCopy(beregningsgrunnlagPeriode);
        BeregningsgrunnlagPeriode.builder(nyBeregningsgrunnlagPeriode)
            .medBeregningsgrunnlagPeriode(nyPeriodeFom, null)
            .leggTilPeriodeÅrsak(nyPeriodeÅrsak).build(beregningsgrunnlag);
    }

    @Test
    public void skalLageToPerioderKortvarigArbeidsforholdHvorTomSammenfallerMedBortfallAvNaturalytelse() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_1.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        BeregningsgrunnlagEntitet nyttGrunnlag = beregningsgrunnlag;
        BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel = nyttGrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()
            .get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(eksisterendeAndel)
            .medBGAndelArbeidsforhold(
                BGAndelArbeidsforhold.builder(eksisterendeAndel.getBgAndelArbeidsforhold().orElse(null)).medTidsbegrensetArbeidsforhold(true))
            .build(nyttGrunnlag.getBeregningsgrunnlagPerioder().get(0));
        var im1 = opprettInntektsmeldingNaturalytelseBortfaller(Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            BigDecimal.valueOf(NATURALYTELSE_I_PERIODE_2), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(4));
        splitBeregningsgrunnlagPeriode(nyttGrunnlag, SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(4), PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        lagKortvarigArbeidsforhold(nyttGrunnlag, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(4).minusDays(1));
        var inntektsmeldinger = List.of(im1);

        // Act
        BeregningsgrunnlagRegelResultat resultat = act(nyttGrunnlag, inntektsmeldinger);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(2);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4).minusDays(1), 1);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null, null);

        periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(1);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4), null, 1, PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET,
            PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1,
            NATURALYTELSE_I_PERIODE_2 * 12, null);
    }

    @Test
    public void skalLageBeregningsgrunnlagMedTrePerioder() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1 + MÅNEDSINNTEKT2), BigDecimal.valueOf(MÅNEDSINNTEKT1 + MÅNEDSINNTEKT2),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_1.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(lagBgAndelArbeidsforhold(ARBEIDSPERIODE_FOM, ARBEIDSPERIODE_TOM, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR2)))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBeregningsperiode(SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(3).withDayOfMonth(1), SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1))
            .build(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0));
        splitBeregningsgrunnlagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2), PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        splitBeregningsgrunnlagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(4), PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        var im1 = opprettInntektsmeldingNaturalytelseBortfaller(Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR2), BigDecimal.valueOf(MÅNEDSINNTEKT2),
            BigDecimal.valueOf(NATURALYTELSE_I_PERIODE_2), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2));
        var im2 = opprettInntektsmeldingNaturalytelseBortfaller(Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            BigDecimal.valueOf(NATURALYTELSE_I_PERIODE_3), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(4));
        var inntektsmeldinger = List.of(im1, im2);

        // Act
        BeregningsgrunnlagRegelResultat resultat = act(beregningsgrunnlag, inntektsmeldinger);

        // Assert
        BeregningsgrunnlagEntitet beregningsgrunnlag = resultat.getBeregningsgrunnlag();
        assertThat(beregningsgrunnlag).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(beregningsgrunnlag.getBeregningsgrunnlagPerioder()).hasSize(3);

        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(2).minusDays(1), 2);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null, null);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(1), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR2), ÅRSINNTEKT2, null, null);

        periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(1);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(2), SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4).minusDays(1), 2,
            PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null, null);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(1), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR2), ÅRSINNTEKT2,
            NATURALYTELSE_I_PERIODE_2 * 12, null);

        periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(2);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(4), null, 2, PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1,
            NATURALYTELSE_I_PERIODE_3 * 12, null);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(1), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR2), ÅRSINNTEKT2,
            NATURALYTELSE_I_PERIODE_2 * 12, null);
    }

    @Test
    public void skalLageBeregningsgrunnlagMedTrePerioderKortvarigFørNaturalytelse() {
        // Arrange
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_1.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(eksisterendeAndel)
            .medBGAndelArbeidsforhold(
                BGAndelArbeidsforhold.builder(eksisterendeAndel.getBgAndelArbeidsforhold().orElse(null)).medTidsbegrensetArbeidsforhold(true))
            .build(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0));
        splitBeregningsgrunnlagPeriode(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(3), PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        lagKortvarigArbeidsforhold(beregningsgrunnlag, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2).minusDays(1));
        Collection<Inntektsmelding> inntektsmeldinger = List.of();

        // Act
        BeregningsgrunnlagRegelResultat resultat = act(beregningsgrunnlag, inntektsmeldinger);

        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(3);

        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(2).minusDays(1), 1);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null, null);

        periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(1);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(2), SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(3).minusDays(1), 1,
            PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null, null);

        periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(2);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING.plusMonths(3), null, 1, PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1, null, null);
    }

    @Test
    public void skalGiEittAksjonspunktForSNNyIArbeidslivetOgKortvarigArbeidsforhold() {
        // Arrange
        BeregningsgrunnlagEntitet nyttGrunnlag = lagBeregningsgrunnlagATFL_SN(true).dypKopi();
        verdikjedeTestHjelper.initBehandlingFor_AT_SN(scenario, BigDecimal.valueOf(12 * MÅNEDSINNTEKT1),
            2014, SKJÆRINGSTIDSPUNKT_BEREGNING, ARBEIDSFORHOLD_ORGNR1,
            BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1));

        behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel = nyttGrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()
            .get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(eksisterendeAndel)
            .medBGAndelArbeidsforhold(
                BGAndelArbeidsforhold.builder(eksisterendeAndel.getBgAndelArbeidsforhold().orElse(null)).medTidsbegrensetArbeidsforhold(true))
            .build(nyttGrunnlag.getBeregningsgrunnlagPerioder().get(0));
        lagKortvarigArbeidsforhold(nyttGrunnlag, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(4).minusDays(1));
        List<Inntektsmelding> inntektsmeldinger = Collections.emptyList();

        // Act
        BeregningsgrunnlagRegelResultat resultat = act(nyttGrunnlag, inntektsmeldinger);

        // Assert
        BeregningsgrunnlagEntitet bg = resultat.getBeregningsgrunnlag();
        assertThat(bg.getBeregningsgrunnlagPerioder()).hasSize(2);
        bg.getBeregningsgrunnlagPerioder().forEach(p -> assertThat(p.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2));
        List<BeregningAksjonspunktResultat> aps = resultat.getAksjonspunkter();
        List<BeregningAksjonspunktDefinisjon> apDefs = aps.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).containsExactly(BeregningAksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET);
    }

    @Test
    public void skalSetteRiktigSammenligningsgrunnlagNårEnPeriodeMedArbeidstakerUtenNaturalytelserOgMedInntektsmeldingNårToggleErPå() {
        // Arrange
        unleash.enable(TOGGLE_SPLITTE_SAMMENLIGNING);
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_2.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), BigDecimal.valueOf(MÅNEDSINNTEKT1+1000), null, null);
        var inntektsmeldinger = List.of(im1);
        // Act
        BeregningsgrunnlagRegelResultat resultat = act(beregningsgrunnlag, inntektsmeldinger);
        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getAksjonspunkter()).isEmpty();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(resultat.getBeregningsgrunnlag().getSammenligningsgrunnlagPrStatusListe()).hasSize(1);
        verifiserSammenligningsgrunnlag(resultat.getBeregningsgrunnlag().getSammenligningsgrunnlagPrStatusListe().get(0), ÅRSINNTEKT1,
            SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(1).minusYears(1).withDayOfMonth(1), SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(1).withDayOfMonth(1).minusDays(1),
            81L, SammenligningsgrunnlagType.SAMMENLIGNING_AT);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verifiserBGAT(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), (MÅNEDSINNTEKT1 + 1000) * 12,
            null, null);
    }

    @Test
    public void skalReturnereAksjonspunktNårArbeidstakerMedInntektsmeldingOgAvvikMellomBeregnetOgSammenligningsgrunnlagUtenTidsbegrensetArbeidsforholdOgToggleErPå() {
        // Arrange
        unleash.enable(TOGGLE_SPLITTE_SAMMENLIGNING);
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_2.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), BigDecimal.valueOf(MÅNEDSINNTEKT1+(MÅNEDSINNTEKT1/2)), null, null);
        var inntektsmeldinger = List.of(im1);
        // Act
        BeregningsgrunnlagRegelResultat resultat = act(beregningsgrunnlag, inntektsmeldinger);
        // Assert
        List<BeregningAksjonspunktResultat> aps = resultat.getAksjonspunkter();
        List<BeregningAksjonspunktDefinisjon> apDefs = aps.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).containsExactly(BeregningAksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
        verifiserSammenligningsgrunnlag(resultat.getBeregningsgrunnlag().getSammenligningsgrunnlagPrStatusListe().get(0), ÅRSINNTEKT1,
            SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(1).minusYears(1).withDayOfMonth(1), SKJÆRINGSTIDSPUNKT_BEREGNING.minusMonths(1).withDayOfMonth(1).minusDays(1),
            500L, SammenligningsgrunnlagType.SAMMENLIGNING_AT);
    }

    @Test
    public void skalSetteRiktigSammenligningsgrunnlagNårEnPeriodeMedFrilanserOgToggleErPå() {
        // Arrange
        unleash.enable(TOGGLE_SPLITTE_SAMMENLIGNING);
        BeregningsgrunnlagEntitet grunnlagFL = lagBeregningsgrunnlagFL();
        BeregningsgrunnlagPrStatusOgAndel.builder(grunnlagFL.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0))
            .build(grunnlagFL.getBeregningsgrunnlagPerioder().get(0));
        behandlingReferanse = lagBehandlingFL(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1), ARBEIDSFORHOLD_ORGNR1);
        Collection<Inntektsmelding> inntektsmeldinger = List.of();
        // Act
        BeregningsgrunnlagRegelResultat resultat = act(grunnlagFL, inntektsmeldinger);
        // Assert
        assertThat(resultat.getBeregningsgrunnlag()).isNotNull();
        assertThat(resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode periode = resultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        assertThat(resultat.getBeregningsgrunnlag().getSammenligningsgrunnlagPrStatusListe()).hasSize(1);
        verifiserSammenligningsgrunnlag(resultat.getBeregningsgrunnlag().getSammenligningsgrunnlagPrStatusListe().get(0),ÅRSINNTEKT1,
            SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1), SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1),
            0, SammenligningsgrunnlagType.SAMMENLIGNING_FL);
        verifiserPeriode(periode, SKJÆRINGSTIDSPUNKT_BEREGNING, null, 1);
        verifiserBGFL(periode.getBeregningsgrunnlagPrStatusOgAndelList().get(0), Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), ÅRSINNTEKT1);
    }

    @Test
    public void skalReturnereAksjonspunktNårFrilanserMedAvvikMellomBeregnetOgSammenligningsgrunnlagOgToggleErPå() {
        // Arrange
        unleash.enable(TOGGLE_SPLITTE_SAMMENLIGNING);
        BeregningsgrunnlagEntitet grunnlagFL = lagBeregningsgrunnlagFL();
        BeregningsgrunnlagPrStatusOgAndel.builder(grunnlagFL.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().get(0))
            .build(grunnlagFL.getBeregningsgrunnlagPerioder().get(0));
        behandlingReferanse = lagBehandlingFL(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(3*MÅNEDSINNTEKT1), ARBEIDSFORHOLD_ORGNR1);
        Collection<Inntektsmelding> inntektsmeldinger = List.of();
        // Act
        BeregningsgrunnlagRegelResultat resultat = act(grunnlagFL, inntektsmeldinger);
        // Assert
        List<BeregningAksjonspunktResultat> aps = resultat.getAksjonspunkter();
        List<BeregningAksjonspunktDefinisjon> apDefs = aps.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).containsExactly(BeregningAksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
        verifiserSammenligningsgrunnlag(resultat.getBeregningsgrunnlag().getSammenligningsgrunnlagPrStatusListe().get(0),ÅRSINNTEKT1,
            SKJÆRINGSTIDSPUNKT_BEREGNING.minusYears(1).withDayOfMonth(1), SKJÆRINGSTIDSPUNKT_BEREGNING.withDayOfMonth(1).minusDays(1),
            2000L, SammenligningsgrunnlagType.SAMMENLIGNING_FL);
    }

    @Test
    public void skalReturnereAksjonspunktNårAtOgEtterTidsbegrensetArbeidsforholdOgToggleErPå() {
        // Arrange
        unleash.enable(TOGGLE_SPLITTE_SAMMENLIGNING);
        behandlingReferanse = lagBehandling(scenario, BigDecimal.valueOf(MÅNEDSINNTEKT1), BigDecimal.valueOf(MÅNEDSINNTEKT1),
            Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), MINUS_YEARS_1.withDayOfMonth(1), MINUS_YEARS_1.withDayOfMonth(1).plusYears(2));
        BeregningsgrunnlagEntitet nyttGrunnlag = beregningsgrunnlag;
        BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel = nyttGrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()
            .get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder(eksisterendeAndel)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder(eksisterendeAndel.getBgAndelArbeidsforhold().orElse(null))
                .medTidsbegrensetArbeidsforhold(true))
            .build(nyttGrunnlag.getBeregningsgrunnlagPerioder().get(0));
        lagKortvarigArbeidsforhold(nyttGrunnlag, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(4).minusDays(1));
        var im1 = verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, Arbeidsgiver.virksomhet(ARBEIDSFORHOLD_ORGNR1), BigDecimal.valueOf(MÅNEDSINNTEKT1 + 10000), null, null);
        Collection<Inntektsmelding> inntektsmeldinger = List.of(im1);
        // Act
        BeregningsgrunnlagRegelResultat resultat = act(beregningsgrunnlag, inntektsmeldinger);

        // Assert
        List<BeregningAksjonspunktResultat> aps = resultat.getAksjonspunkter();
        List<BeregningAksjonspunktDefinisjon> apDefs = aps.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).containsExactly(BeregningAksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
    }

    private BehandlingReferanse lagReferanseMedStp(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(
            Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_BEREGNING)
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
                .build());
    }

    private BeregningsgrunnlagRegelResultat act(BeregningsgrunnlagInput input, BeregningsgrunnlagEntitet beregningsgrunnlag) {
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medRegisterAktiviteter(BeregningAktivitetTestUtil.opprettBeregningAktiviteter(SKJÆRINGSTIDSPUNKT_OPPTJENING, OpptjeningAktivitetType.ARBEID))
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        var newInput = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        return tjeneste.foreslåBeregningsgrunnlag(newInput, grunnlag);
    }

    private void verifiserPeriode(BeregningsgrunnlagPeriode periode, LocalDate fom, LocalDate tom, int antallAndeler,
                                  PeriodeÅrsak... forventedePeriodeÅrsaker) {
        assertThat(periode.getBeregningsgrunnlagPeriodeFom()).isEqualTo(fom);
        assertThat(periode.getBeregningsgrunnlagPeriodeTom()).isEqualTo(tom);
        assertThat(periode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(antallAndeler);
        assertThat(periode.getPeriodeÅrsaker()).containsExactlyInAnyOrder(forventedePeriodeÅrsaker);
    }

    private void verifiserSammenligningsgrunnlag(Sammenligningsgrunnlag sammenligningsgrunnlag, double rapportertPrÅr, LocalDate fom,
                                                 LocalDate tom, long avvikPromille) {
        assertThat(sammenligningsgrunnlag.getRapportertPrÅr().doubleValue()).isEqualTo(rapportertPrÅr);
        assertThat(sammenligningsgrunnlag.getSammenligningsperiodeFom()).isEqualTo(fom);
        assertThat(sammenligningsgrunnlag.getSammenligningsperiodeTom()).isEqualTo(tom);
        assertThat(sammenligningsgrunnlag.getAvvikPromille()).isEqualTo(avvikPromille);
    }

    private void verifiserSammenligningsgrunnlag(SammenligningsgrunnlagPrStatus sammenligningsgrunnlagPrStatus, double rapportertPrÅr, LocalDate fom,
                                                 LocalDate tom, long avvikPromille, SammenligningsgrunnlagType sammenligningsgrunnlagType) {
        assertThat(sammenligningsgrunnlagPrStatus.getRapportertPrÅr().doubleValue()).isEqualTo(rapportertPrÅr);
        assertThat(sammenligningsgrunnlagPrStatus.getSammenligningsperiodeFom()).isEqualTo(fom);
        assertThat(sammenligningsgrunnlagPrStatus.getSammenligningsperiodeTom()).isEqualTo(tom);
        assertThat(sammenligningsgrunnlagPrStatus.getAvvikPromille()).isEqualTo(avvikPromille);
        assertThat(sammenligningsgrunnlagPrStatus.getSammenligningsgrunnlagType()).isEqualTo(sammenligningsgrunnlagType);
    }

    private void verifiserBGAT(BeregningsgrunnlagPrStatusOgAndel bgpsa, Arbeidsgiver arbeidsgiver, double årsinntekt,
                               Double naturalytelseBortfaltPrÅr, Double naturalytelseTilkommerPrÅr) {
        assertThat(bgpsa.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        assertThat(bgpsa.getInntektskategori()).isEqualTo(Inntektskategori.ARBEIDSTAKER);
        assertThat(bgpsa.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver))
            .hasValueSatisfying(virk -> assertThat(virk).isEqualTo(arbeidsgiver));
        assertThat(bgpsa.getBgAndelArbeidsforhold()
            .map(BGAndelArbeidsforhold::getArbeidsforholdRef)
            .map(InternArbeidsforholdRef::gjelderForSpesifiktArbeidsforhold).orElse(false))
                .as("gjelderSpesifiktArbeidsforhold").isFalse();
        assertThat(bgpsa.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
        assertThat(bgpsa.getAvkortetPrÅr()).isNull();
        assertThat(bgpsa.getBeregnetPrÅr().doubleValue()).isEqualTo(årsinntekt);
        assertThat(bgpsa.getBruttoPrÅr().doubleValue()).isEqualTo(årsinntekt);
        assertThat(bgpsa.getOverstyrtPrÅr()).isNull();
        if (naturalytelseBortfaltPrÅr == null) {
            assertThat(bgpsa.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr)).as("naturalytelseBortfalt").isEmpty();
        } else {
            assertThat(bgpsa.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr)).as("naturalytelseBortfalt")
                .hasValueSatisfying(naturalytelse -> assertThat(naturalytelse.doubleValue()).isEqualTo(naturalytelseBortfaltPrÅr));
        }
        if (naturalytelseTilkommerPrÅr == null) {
            assertThat(bgpsa.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseTilkommetPrÅr)).as("naturalytelseTilkommer").isEmpty();
        } else {
            assertThat(bgpsa.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseTilkommetPrÅr)).as("naturalytelseTilkommer")
                .hasValueSatisfying(naturalytelse -> assertThat(naturalytelse.doubleValue()).isEqualTo(naturalytelseTilkommerPrÅr));
        }
        assertThat(bgpsa.getRedusertPrÅr()).isNull();
    }

    private void verifiserBGFL(BeregningsgrunnlagPrStatusOgAndel bgpsa, Arbeidsgiver arbeidsgiver, double årsinntekt) {
        assertThat(bgpsa.getAktivitetStatus()).isEqualTo(AktivitetStatus.FRILANSER);
        assertThat(bgpsa.getInntektskategori()).isEqualTo(Inntektskategori.FRILANSER);
        assertThat(bgpsa.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver))
            .hasValueSatisfying(virk -> assertThat(virk).isEqualTo(arbeidsgiver));
        assertThat(bgpsa.getBgAndelArbeidsforhold()
            .map(BGAndelArbeidsforhold::getArbeidsforholdRef)
            .map(InternArbeidsforholdRef::gjelderForSpesifiktArbeidsforhold).orElse(false))
                .as("gjelderSpesifiktArbeidsforhold").isFalse();
        assertThat(bgpsa.getArbeidsforholdType()).isEqualTo(OpptjeningAktivitetType.FRILANS);
        assertThat(bgpsa.getAvkortetPrÅr()).isNull();
        assertThat(bgpsa.getBeregnetPrÅr().doubleValue()).isEqualTo(årsinntekt);
        assertThat(bgpsa.getBruttoPrÅr().doubleValue()).as("BruttoPrÅr").isEqualTo(årsinntekt);
        assertThat(bgpsa.getOverstyrtPrÅr()).as("OverstyrtPrÅr").isNull();
        assertThat(bgpsa.getRedusertPrÅr()).isNull();
    }

    private Inntektsmelding opprettInntektsmeldingNaturalytelseBortfaller(Arbeidsgiver arbeidsgiver, BigDecimal inntektInntektsmelding, BigDecimal naturalytelseBortfaller,
                                                               LocalDate naturalytelseBortfallerDato) {
        return verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, arbeidsgiver, inntektInntektsmelding,
            new NaturalYtelse(Tid.TIDENES_BEGYNNELSE, naturalytelseBortfallerDato.minusDays(1), naturalytelseBortfaller, NaturalYtelseType.ANNET),
            null);
    }

    private Inntektsmelding opprettInntektsmeldingNaturalytelseTilkommer(Arbeidsgiver arbeidsgiver, BigDecimal inntektInntektsmelding, BigDecimal naturalytelseTilkommer,
                                                              LocalDate naturalytelseTilkommerDato) {
        return verdikjedeTestHjelper.opprettInntektsmeldingMedRefusjonskrav(behandlingReferanse, arbeidsgiver, inntektInntektsmelding,
            new NaturalYtelse(naturalytelseTilkommerDato, Tid.TIDENES_ENDE, naturalytelseTilkommer, NaturalYtelseType.ANNET), null);
    }
}
