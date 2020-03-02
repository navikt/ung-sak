package no.nav.folketrygdloven.beregningsgrunnlag;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.util.BeregningsgrunnlagTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetOverstyringEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetOverstyringerEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.refusjon.InntektsmeldingMedRefusjonTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningInntektsmeldingTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.BekreftetPermisjonStatus;
import no.nav.k9.kodeverk.arbeidsforhold.NaturalYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAktivitetHandlingType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.Hjemmel;
import no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.konfig.Tid;

public class FastsettBeregningsgrunnlagPerioderTjenesteImplTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, Month.JANUARY, 4);
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000L);
    private static final String ORG_NUMMER = "915933149";
    private static final String ORG_NUMMER_2 = "974760673";
    private static final String ORG_NUMMER_3 = "976967631";

    private static final AktørId ARBEIDSGIVER_AKTØR_ID = AktørId.dummy();
    private static final BigDecimal ANTALL_MÅNEDER_I_ÅR = BigDecimal.valueOf(12);
    private static final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
        .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
        .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
        .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT)
        .build();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(entityManager);
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);

    private final BeregningIAYTestUtil iayTestUtil = new BeregningIAYTestUtil(iayTjeneste);

    private final BeregningInntektsmeldingTestUtil inntektsmeldingTestUtil = new BeregningInntektsmeldingTestUtil(inntektsmeldingTjeneste);

    private final BeregningsgrunnlagTestUtil beregningTestUtil = new BeregningsgrunnlagTestUtil(beregningsgrunnlagRepository, iayTjeneste);

    private BeregningAktivitetAggregatEntitet beregningAktivitetAggregat;
    private List<BeregningAktivitetEntitet> aktiviteter = new ArrayList<>();

    private final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);
    private final Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet(ORG_NUMMER_2);

    private FastsettBeregningsgrunnlagPerioderTjeneste tjeneste;

    private BehandlingReferanse behandlingRef;
    private TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
    private static final DatoIntervallEntitet ARBEIDSPERIODE = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), TIDENES_ENDE);

    @Before
    public void setUp() {
        tjeneste = lagTjeneste();
        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, List.of(ORG_NUMMER));

    }

    private FastsettBeregningsgrunnlagPerioderTjeneste lagTjeneste() {
        InntektsmeldingMedRefusjonTjeneste finnFørste = new InntektsmeldingMedRefusjonTjeneste(inntektsmeldingTjeneste);
        var oversetterTilRegelNaturalytelse = new MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse(finnFørste);
        var oversetterTilRegelRefusjonOgGradering = new MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering(finnFørste);
        var oversetterFraRegelTilVLNaturalytelse = new MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse();
        MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering oversetterFraRegelTilVLRefusjonOgGradering = new MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering();
        return new FastsettBeregningsgrunnlagPerioderTjeneste(oversetterTilRegelNaturalytelse,
            new UnitTestLookupInstanceImpl<>(oversetterTilRegelRefusjonOgGradering), oversetterFraRegelTilVLNaturalytelse,
            oversetterFraRegelTilVLRefusjonOgGradering);
    }

    private void leggTilYrkesaktiviteterOgBeregningAktiviteter(AbstractTestScenario<?> scenario, List<String> orgnrs) {

        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(scenario.getSøkerAktørId());
        for (String orgnr : orgnrs) {
            Arbeidsgiver arbeidsgiver = leggTilYrkesaktivitet(ARBEIDSPERIODE, aktørArbeidBuilder, orgnr);
            fjernOgLeggTilNyBeregningAktivitet(ARBEIDSPERIODE.getFomDato(), ARBEIDSPERIODE.getTomDato(), arbeidsgiver, InternArbeidsforholdRef.nullRef());
        }

        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);
    }

    private Arbeidsgiver leggTilYrkesaktivitet(DatoIntervallEntitet arbeidsperiode, InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder,
                                               String orgnr) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        AktivitetsAvtaleBuilder aaBuilder1 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medPeriode(arbeidsperiode);
        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aaBuilder1);
        aktørArbeidBuilder.leggTilYrkesaktivitet(yaBuilder);
        return arbeidsgiver;
    }

    private void fjernOgLeggTilNyBeregningAktivitet(LocalDate fom, LocalDate tom, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        if (fom.isAfter(SKJÆRINGSTIDSPUNKT)) {
            throw new IllegalArgumentException("Kan ikke lage BeregningAktivitet som starter etter skjæringstidspunkt");
        }
        fjernAktivitet(arbeidsgiver, arbeidsforholdRef);
        aktiviteter.add(lagAktivitet(fom, tom, arbeidsgiver, arbeidsforholdRef));
        lagAggregatEntitetFraListe(aktiviteter);
    }

    private void fjernAktivitet(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        aktiviteter.stream()
            .filter(a -> a.gjelderFor(arbeidsgiver, arbeidsforholdRef)).findFirst()
            .ifPresent(a -> aktiviteter.remove(a));
        lagAggregatEntitetFraListe(aktiviteter);
    }

    private void lagAggregatEntitetFraListe(List<BeregningAktivitetEntitet> aktiviteter) {
        BeregningAktivitetAggregatEntitet.Builder builder = BeregningAktivitetAggregatEntitet.builder();
        aktiviteter.forEach(builder::leggTilAktivitet);
        beregningAktivitetAggregat = builder.medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT).build();
    }

    private BeregningAktivitetEntitet lagAktivitet(LocalDate fom, LocalDate tom, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return BeregningAktivitetEntitet.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(arbeidsforholdRef)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .build();
    }

    @Test
    public void ikkeLagPeriodeForRefusjonHvisKunEnInntektsmeldingIngenEndringIRefusjon() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(23987);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, SKJÆRINGSTIDSPUNKT.atStartOfDay());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, null);
        Optional<BGAndelArbeidsforhold> bgaOpt = finnBGAndelArbeidsforhold(perioder.get(0), ORG_NUMMER);
        assertThat(bgaOpt).hasValueSatisfying(bga -> assertThat(bga.getRefusjonskravPrÅr()).isEqualByComparingTo(inntekt.multiply(ANTALL_MÅNEDER_I_ÅR)));
    }

    private BeregningsgrunnlagEntitet fastsettPerioderForRefusjonOgGradering(BehandlingReferanse ref,
                                                                             BeregningsgrunnlagGrunnlagEntitet grunnlag,
                                                                             BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                             AktivitetGradering aktivitetGradering,
                                                                             List<Inntektsmelding> inntektsmeldinger) {
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(ref.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, null, aktivitetGradering, null)
            .medBeregningsgrunnlagGrunnlag(grunnlag);
        return tjeneste.fastsettPerioderForRefusjonOgGradering(input, beregningsgrunnlag);
    }

    private BeregningsgrunnlagEntitet fastsettPerioderForNaturalytelse(BehandlingReferanse ref,
                                                                       BeregningsgrunnlagGrunnlagEntitet grunnlag,
                                                                       BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                       AktivitetGradering aktivitetGradering,
                                                                       List<Inntektsmelding> inntektsmeldinger) {
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(ref.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var input = new BeregningsgrunnlagInput(ref, iayGrunnlag, null, aktivitetGradering, null)
            .medBeregningsgrunnlagGrunnlag(grunnlag);
        return tjeneste.fastsettPerioderForNaturalytelse(input, beregningsgrunnlag);
    }

    @Test
    public void lagPeriodeForRefusjonHvisKunEnInntektsmeldingIngenEndringIRefusjonArbeidsgiverSøkerForSent() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(23987);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt,
            LocalDate.of(2019, Month.MAY, 2).atStartOfDay());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, LocalDate.of(2019, Month.JANUARY, 31));
        assertThat(finnBGAndelArbeidsforhold(perioder.get(0), ORG_NUMMER))
            .hasValueSatisfying(bga -> assertThat(bga.getRefusjonskravPrÅr()).isEqualByComparingTo(BigDecimal.ZERO));
        assertBeregningsgrunnlagPeriode(perioder.get(1), LocalDate.of(2019, Month.FEBRUARY, 1), null, PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV);
        assertThat(finnBGAndelArbeidsforhold(perioder.get(1), ORG_NUMMER))
            .hasValueSatisfying(bga -> assertThat(bga.getRefusjonskravPrÅr()).isEqualByComparingTo(inntekt.multiply(ANTALL_MÅNEDER_I_ÅR)));
    }

    @Test
    public void ikkeLagPeriodeForZeroRefusjonHvisKunEnInntektsmeldingIngenEndringIRefusjon() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(23987);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, BigDecimal.ZERO, SKJÆRINGSTIDSPUNKT.atStartOfDay());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, null);
    }

    @Test
    public void lagPeriodeForNaturalytelseTilkommer() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(40000);
        NaturalYtelse naturalYtelseTilkommer = new NaturalYtelse(SKJÆRINGSTIDSPUNKT.plusDays(30), TIDENES_ENDE, BigDecimal.valueOf(350),
            NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmeldingMedNaturalYtelser(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, null,
            SKJÆRINGSTIDSPUNKT.atStartOfDay(), naturalYtelseTilkommer);

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForNaturalytelse(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(29));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusDays(30), null, PeriodeÅrsak.NATURALYTELSE_TILKOMMER);
    }

    @Test
    public void lagPeriodeForNaturalytelseBortfalt() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(40000);
        NaturalYtelse naturalYtelseBortfall = new NaturalYtelse(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusDays(30),
            BigDecimal.valueOf(350), NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmeldingMedNaturalYtelser(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, null,
            SKJÆRINGSTIDSPUNKT.atStartOfDay(), naturalYtelseBortfall);

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForNaturalytelse(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(30));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusDays(31), null, PeriodeÅrsak.NATURALYTELSE_BORTFALT);
    }

    @Test
    public void ikkeLagPeriodeForNaturalytelseBortfaltPåStp() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(40000);
        NaturalYtelse naturalYtelseBortfall = new NaturalYtelse(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.minusDays(1),
            BigDecimal.valueOf(350), NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmeldingMedNaturalYtelser(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, null,
            SKJÆRINGSTIDSPUNKT.atStartOfDay(), naturalYtelseBortfall);

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForNaturalytelse(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, null);
    }

    @Test
    public void lagPeriodeForNaturalytelseBortfaltDagenEtterStp() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(40000);
        NaturalYtelse naturalYtelseBortfall = new NaturalYtelse(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT, BigDecimal.valueOf(350),
            NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmeldingMedNaturalYtelser(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, null,
            SKJÆRINGSTIDSPUNKT.atStartOfDay(), naturalYtelseBortfall);

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForNaturalytelse(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT);
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusDays(1), null, PeriodeÅrsak.NATURALYTELSE_BORTFALT);
    }

    @Test
    public void lagPerioderForNaturalytelseBortfaltOgTilkommer() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(40000);
        NaturalYtelse naturalYtelseBortfalt = new NaturalYtelse(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusDays(30),
            BigDecimal.valueOf(350), NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON);
        NaturalYtelse naturalYtelseTilkommer = new NaturalYtelse(SKJÆRINGSTIDSPUNKT.plusDays(90), TIDENES_ENDE, BigDecimal.valueOf(350),
            NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmeldingMedNaturalYtelser(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, null,
            SKJÆRINGSTIDSPUNKT.atStartOfDay(), naturalYtelseBortfalt, naturalYtelseTilkommer);

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForNaturalytelse(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(3);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(30));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusDays(31), SKJÆRINGSTIDSPUNKT.plusDays(89), PeriodeÅrsak.NATURALYTELSE_BORTFALT);
        assertBeregningsgrunnlagPeriode(perioder.get(2), SKJÆRINGSTIDSPUNKT.plusDays(90), null, PeriodeÅrsak.NATURALYTELSE_TILKOMMER);
    }

    @Test
    public void lagPeriodeForRefusjonOpphører() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(40000);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, SKJÆRINGSTIDSPUNKT.plusDays(100),
            SKJÆRINGSTIDSPUNKT.atStartOfDay());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));
        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(100));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusDays(101), null, PeriodeÅrsak.REFUSJON_OPPHØRER);
    }

    @Test
    public void lagPeriodeForGraderingLik6G() {
        // Arrange
        LocalDate refusjonOpphørerDato = SKJÆRINGSTIDSPUNKT.plusWeeks(9).minusDays(1);
        LocalDate graderingFom = refusjonOpphørerDato.plusDays(1);
        LocalDate graderingTom = SKJÆRINGSTIDSPUNKT.plusWeeks(18).minusDays(1);

        var scenario = TestScenarioBuilder.nyttScenario();

        var arbeidsgiverGradering = Arbeidsgiver.virksomhet(ORG_NUMMER);

        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, List.of(ORG_NUMMER, ORG_NUMMER_2));
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER, ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_2, SKJÆRINGSTIDSPUNKT, GRUNNBELØP.multiply(BigDecimal.valueOf(6)), inntekt1,
            SKJÆRINGSTIDSPUNKT.atStartOfDay());

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiverGradering)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag, aktivitetGradering, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(3);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusWeeks(9).minusDays(1));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusWeeks(9), graderingTom, PeriodeÅrsak.GRADERING);
        assertBeregningsgrunnlagPeriode(perioder.get(2), graderingTom.plusDays(1), null, PeriodeÅrsak.GRADERING_OPPHØRER);
    }

    @Test
    public void lagPeriodeForGraderingSN_refusjon_over_6G() {
        // Arrange
        LocalDate graderingFom = SKJÆRINGSTIDSPUNKT.plusWeeks(9);
        LocalDate graderingTom = SKJÆRINGSTIDSPUNKT.plusWeeks(18).minusDays(1);

        var scenario = TestScenarioBuilder.nyttScenario();

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, List.of(ORG_NUMMER, ORG_NUMMER_2));
        var behandlingReferanse = lagre(scenario);

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_2, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1, SKJÆRINGSTIDSPUNKT
            .atStartOfDay());
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER, ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0));

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag, aktivitetGradering, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(3);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, graderingFom.minusDays(1));
        assertBeregningsgrunnlagPeriode(perioder.get(1), graderingFom, graderingTom, PeriodeÅrsak.GRADERING);
        assertBeregningsgrunnlagPeriode(perioder.get(2), graderingTom.plusDays(1), null, PeriodeÅrsak.GRADERING_OPPHØRER);
    }

    @Test
    public void lagPeriodeForGraderingSN_bg_over_6g() {
        // Arrange
        LocalDate graderingFom = SKJÆRINGSTIDSPUNKT.plusWeeks(9);
        LocalDate graderingTom = SKJÆRINGSTIDSPUNKT.plusWeeks(18).minusDays(1);

        var scenario = TestScenarioBuilder.nyttScenario();

        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, List.of(ORG_NUMMER_2));
        var behandlingReferanse = lagre(scenario);

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BeregningsgrunnlagPeriode bgPeriode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(bgPeriode);
        BeregningsgrunnlagPrStatusOgAndel.builder(bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(0))
            .medBeregnetPrÅr(inntekt1.multiply(ANTALL_MÅNEDER_I_ÅR));

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag, aktivitetGradering, List.of());

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusWeeks(9).minusDays(1));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusWeeks(9), null, PeriodeÅrsak.GRADERING);
    }

    @Test
    public void lagPeriodeForGraderingOver6GOgOpphørRefusjonSammeDag() {
        // Arrange
        LocalDate refusjonOpphørerDato = SKJÆRINGSTIDSPUNKT.plusWeeks(9).minusDays(1);
        LocalDate graderingFom = refusjonOpphørerDato.plusDays(1);
        LocalDate graderingTom = Tid.TIDENES_ENDE;

        var scenario = TestScenarioBuilder.nyttScenario();

        var arbeidsgiverGradering = Arbeidsgiver.virksomhet(ORG_NUMMER);

        List<String> orgnrs = List.of(ORG_NUMMER, ORG_NUMMER_3, ORG_NUMMER_2);
        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, orgnrs);
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(orgnrs, behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_3, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1,
            SKJÆRINGSTIDSPUNKT.atStartOfDay());
        var im2 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_2, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1, refusjonOpphørerDato,
            SKJÆRINGSTIDSPUNKT.atStartOfDay().plusSeconds(1));

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiverGradering)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());
        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            aktivitetGradering, List.of(im1, im2));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, refusjonOpphørerDato);
        assertBeregningsgrunnlagPeriode(perioder.get(1), graderingFom, null, PeriodeÅrsak.GRADERING, PeriodeÅrsak.REFUSJON_OPPHØRER);

    }

    @Test
    public void lagPeriodeForGraderingOver6GFL() {
        // Arrange
        LocalDate refusjonOpphørerDato = SKJÆRINGSTIDSPUNKT.plusWeeks(9).minusDays(1);
        LocalDate graderingFom = refusjonOpphørerDato.plusDays(1);
        LocalDate graderingTom = SKJÆRINGSTIDSPUNKT.plusWeeks(18).minusDays(1);

        var scenario = TestScenarioBuilder.nyttScenario();

        var arbeidsgiverGradering = Arbeidsgiver.virksomhet(ORG_NUMMER);

        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, List.of(ORG_NUMMER, ORG_NUMMER_2));
        var behandlingReferanse = lagre(scenario);

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_2, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1, SKJÆRINGSTIDSPUNKT
            .atStartOfDay());
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER, ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medBeregningsperiode(SKJÆRINGSTIDSPUNKT, null)
            .build(beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0));

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiverGradering)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag, aktivitetGradering, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(3);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusWeeks(9).minusDays(1));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusWeeks(9), graderingTom, PeriodeÅrsak.GRADERING);
        assertBeregningsgrunnlagPeriode(perioder.get(2), graderingTom.plusDays(1), null, PeriodeÅrsak.GRADERING_OPPHØRER);

    }

    @Test
    public void lagPeriodeForGraderingArbeidsforholdTilkomEtterStp() {
        // Arrange
        LocalDate graderingFom = SKJÆRINGSTIDSPUNKT.plusWeeks(9);
        LocalDate graderingTom = SKJÆRINGSTIDSPUNKT.plusWeeks(18).minusDays(1);

        var scenario = TestScenarioBuilder.nyttScenario();

        var arbeidsgiverGradering = Arbeidsgiver.virksomhet(ORG_NUMMER);

        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        var arbId = InternArbeidsforholdRef.namedRef("A");
        Arbeidsgiver arbeidsgiver3 = Arbeidsgiver.virksomhet(ORG_NUMMER);
        Arbeidsgiver arbeidsgiver4 = Arbeidsgiver.virksomhet(ORG_NUMMER_2);
        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusWeeks(9), SKJÆRINGSTIDSPUNKT.plusMonths(5).minusDays(2),
            arbId, arbeidsgiver3, BigDecimal.TEN);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_2, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1,
            SKJÆRINGSTIDSPUNKT.atStartOfDay());
        fjernAktivitet(arbeidsgiver3, arbId);
        fjernOgLeggTilNyBeregningAktivitet(SKJÆRINGSTIDSPUNKT.minusYears(2), TIDENES_ENDE, arbeidsgiver4, arbId);

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiverGradering)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag, aktivitetGradering, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusWeeks(9).minusDays(1));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusWeeks(9), null, PeriodeÅrsak.GRADERING);
        assertThat(perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        assertThat(finnBGAndelArbeidsforhold(perioder.get(0), ORG_NUMMER_2)).isPresent();
        assertThat(perioder.get(1).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
    }

    @Test
    public void lagPeriodeForRefusjonArbeidsforholdTilkomEtterStp() {
        var arbId = InternArbeidsforholdRef.namedRef("A");

        // Arrange
        var scenario = TestScenarioBuilder.nyttScenario();
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        LocalDate ansettelsesDato = SKJÆRINGSTIDSPUNKT.plusWeeks(2);
        LocalDate startDatoRefusjon = SKJÆRINGSTIDSPUNKT.plusWeeks(1);
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, ansettelsesDato, ansettelsesDato.plusMonths(5).minusDays(2), arbId, arbeidsgiver,
            BigDecimal.TEN);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, startDatoRefusjon, BigDecimal.valueOf(20000), BigDecimal.valueOf(20000),
            SKJÆRINGSTIDSPUNKT.atStartOfDay());
        fjernAktivitet(arbeidsgiver, arbId);

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, ansettelsesDato.minusDays(1));
        assertBeregningsgrunnlagPeriode(perioder.get(1), ansettelsesDato, null, PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV);
        assertThat(perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList()).isEmpty();
        assertThat(perioder.get(1).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
    }

    @Test
    public void ikkeLagAndelForRefusjonForArbeidsforholdSomBortfallerFørSkjæringstidspunkt() {
        var arbId = InternArbeidsforholdRef.namedRef("A");

        // Arrange
        var scenario = TestScenarioBuilder.nyttScenario();
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        LocalDate startDatoRefusjon = SKJÆRINGSTIDSPUNKT.plusWeeks(1);
        LocalDate ansattFom = SKJÆRINGSTIDSPUNKT.minusYears(2);
        LocalDate ansattTom = SKJÆRINGSTIDSPUNKT.minusMonths(2);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, ansattFom, ansattTom, arbId, Arbeidsgiver.virksomhet(ORG_NUMMER), BigDecimal.TEN);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, startDatoRefusjon, BigDecimal.valueOf(20000), BigDecimal.valueOf(20000),
            SKJÆRINGSTIDSPUNKT.atStartOfDay());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList()).isEmpty();
    }

    @Test
    public void lagPeriodeForRefusjonArbeidsforholdTilkomEtterStpFlerePerioder() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        var arbId = InternArbeidsforholdRef.namedRef("A");
        Arbeidsgiver arbeidsgiver3 = Arbeidsgiver.virksomhet(ORG_NUMMER);

        LocalDate startDatoPermisjon = SKJÆRINGSTIDSPUNKT.plusWeeks(1);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5),
            arbId, Arbeidsgiver.virksomhet(ORG_NUMMER));
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, startDatoPermisjon, SKJÆRINGSTIDSPUNKT.plusMonths(5).minusDays(2),
            arbId, Arbeidsgiver.virksomhet(ORG_NUMMER_2));

        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, BigDecimal.valueOf(30000), BigDecimal.valueOf(30000),
            SKJÆRINGSTIDSPUNKT.plusWeeks(12), SKJÆRINGSTIDSPUNKT.atStartOfDay());
        var im2 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_2, startDatoPermisjon, BigDecimal.valueOf(20000), BigDecimal.valueOf(20000),
            SKJÆRINGSTIDSPUNKT.atStartOfDay().plusSeconds(1));
        fjernOgLeggTilNyBeregningAktivitet(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5), arbeidsgiver3, arbId);

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1, im2));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(3);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, startDatoPermisjon.minusDays(1));
        assertBeregningsgrunnlagPeriode(perioder.get(1), startDatoPermisjon, SKJÆRINGSTIDSPUNKT.plusWeeks(12), PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV);
        assertBeregningsgrunnlagPeriode(perioder.get(2), SKJÆRINGSTIDSPUNKT.plusWeeks(12).plusDays(1), null, PeriodeÅrsak.REFUSJON_OPPHØRER);
        assertThat(perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        assertThat(perioder.get(1).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        assertThat(perioder.get(2).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        var referanse = scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening)
            .medSkjæringstidspunkt(skjæringstidspunkt);
        this.behandlingRef = referanse;
        return referanse;
    }

    @Test
    public void skalSetteRefusjonskravForSøktRefusjonFraSkjæringstidspunktUtenOpphørsdato() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        var arbId = InternArbeidsforholdRef.namedRef("A");
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5), arbId,
            Arbeidsgiver.virksomhet(ORG_NUMMER));
        List<LocalDateInterval> berPerioder = singletonList(new LocalDateInterval(SKJÆRINGSTIDSPUNKT, null));
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingRef, SKJÆRINGSTIDSPUNKT,
            berPerioder);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1, SKJÆRINGSTIDSPUNKT.atStartOfDay());

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(refusjonskrav1.multiply(ANTALL_MÅNEDER_I_ÅR));
    }

    @Test
    public void skalSetteRefusjonskravForSøktRefusjonFraSkjæringstidspunktUtenOpphørsdatoPrivatpersonSomArbeidsgiver() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        var arbId = InternArbeidsforholdRef.namedRef("A");
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.person(ARBEIDSGIVER_AKTØR_ID);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT,
            SKJÆRINGSTIDSPUNKT.minusYears(2),
            SKJÆRINGSTIDSPUNKT.plusYears(5), arbId, arbeidsgiver);
        List<LocalDateInterval> berPerioder = singletonList(new LocalDateInterval(SKJÆRINGSTIDSPUNKT, null));
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingRef,
            SKJÆRINGSTIDSPUNKT, berPerioder);
        BeregningAktivitetEntitet aktivitetEntitet = BeregningAktivitetEntitet.builder().medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(arbId)
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5))).build();
        beregningAktivitetAggregat = leggTilAktivitet(aktivitetEntitet);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, arbeidsgiver, arbId, SKJÆRINGSTIDSPUNKT,
            emptyList(),
            refusjonskrav1, inntekt1, null, emptyList(), emptyList(), SKJÆRINGSTIDSPUNKT.atStartOfDay());

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(refusjonskrav1.multiply(ANTALL_MÅNEDER_I_ÅR));
    }

    private BeregningAktivitetAggregatEntitet leggTilAktivitet(BeregningAktivitetEntitet aktivitetEntitet) {
        aktiviteter.add(aktivitetEntitet);
        BeregningAktivitetAggregatEntitet.Builder builder = BeregningAktivitetAggregatEntitet.builder();
        aktiviteter.forEach(builder::leggTilAktivitet);
        return builder.medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT).build();
    }

    @Test
    public void skalSetteRefusjonskravForSøktRefusjonFraSkjæringstidspunktMedOpphørsdato() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        var arbId = InternArbeidsforholdRef.namedRef("A");
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT,
            SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5), arbId, arbeidsgiver);
        List<LocalDateInterval> berPerioder = singletonList(new LocalDateInterval(SKJÆRINGSTIDSPUNKT, null));
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingRef, SKJÆRINGSTIDSPUNKT,
            berPerioder);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        LocalDate refusjonOpphørerDato = SKJÆRINGSTIDSPUNKT.plusWeeks(6).minusDays(1);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1, refusjonOpphørerDato,
            SKJÆRINGSTIDSPUNKT.atStartOfDay());
        fjernOgLeggTilNyBeregningAktivitet(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5), arbeidsgiver, arbId);

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        assertThat(perioder.get(0).getBeregningsgrunnlagPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        List<BeregningsgrunnlagPrStatusOgAndel> andelerIFørstePeriode = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andelerIFørstePeriode).hasSize(1);
        assertThat(andelerIFørstePeriode.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(refusjonskrav1.multiply(ANTALL_MÅNEDER_I_ÅR));

        assertThat(perioder.get(1).getBeregningsgrunnlagPeriodeFom()).isEqualTo(refusjonOpphørerDato.plusDays(1));
        List<BeregningsgrunnlagPrStatusOgAndel> andelerIAndrePeriode = perioder.get(1).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andelerIAndrePeriode).hasSize(1);
        assertThat(andelerIAndrePeriode.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    public void skalIkkeSetteRefusjonForAktivitetSomErFjernetIOverstyring() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        BeregningAktivitetHandlingType handlingIkkeBenytt = BeregningAktivitetHandlingType.IKKE_BENYTT;
        BeregningAktivitetOverstyringerEntitet overstyring = BeregningAktivitetOverstyringerEntitet.builder()
            .leggTilOverstyring(lagOverstyringForAktivitet(InternArbeidsforholdRef.nullRef(), arbeidsgiver, handlingIkkeBenytt)).build();
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag(List.of(), behandlingReferanse, beregningAktivitetAggregat, overstyring);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt1, inntekt1,
            null, SKJÆRINGSTIDSPUNKT.atStartOfDay());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getBeregningsgrunnlagPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        List<BeregningsgrunnlagPrStatusOgAndel> andelerIFørstePeriode = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andelerIFørstePeriode).hasSize(0);
    }

    @Test
    public void skalSetteRefusjonForAktivitetSomErFjernetISaksbehandlet() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedSaksbehandlet(List.of(), behandlingReferanse, beregningAktivitetAggregat,
            BeregningAktivitetAggregatEntitet.builder().medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT).build());
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt1, inntekt1,
            null, SKJÆRINGSTIDSPUNKT.atStartOfDay());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getBeregningsgrunnlagPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        List<BeregningsgrunnlagPrStatusOgAndel> andelerIFørstePeriode = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andelerIFørstePeriode).hasSize(1);
    }

    @Test
    public void skalSetteRefusjonskravForSøktRefusjonFraEtterSkjæringstidspunktMedOpphørsdato() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        var arbId = InternArbeidsforholdRef.namedRef("A");
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5), arbId,
            Arbeidsgiver.virksomhet(ORG_NUMMER));
        List<LocalDateInterval> berPerioder = singletonList(new LocalDateInterval(SKJÆRINGSTIDSPUNKT, null));
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingRef, SKJÆRINGSTIDSPUNKT,
            berPerioder);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        LocalDate refusjonOpphørerDato = SKJÆRINGSTIDSPUNKT.plusWeeks(6).minusDays(1);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1, refusjonOpphørerDato,
            SKJÆRINGSTIDSPUNKT.atStartOfDay());
        fjernOgLeggTilNyBeregningAktivitet(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5), arbeidsgiver, arbId);

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        assertThat(perioder.get(0).getBeregningsgrunnlagPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        List<BeregningsgrunnlagPrStatusOgAndel> andelerIFørstePeriode = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andelerIFørstePeriode).hasSize(1);
        assertThat(andelerIFørstePeriode.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(refusjonskrav1.multiply(ANTALL_MÅNEDER_I_ÅR));
        assertThat(perioder.get(1).getBeregningsgrunnlagPeriodeFom()).isEqualTo(refusjonOpphørerDato.plusDays(1));
        assertThat(perioder.get(1).getPeriodeÅrsaker()).isEqualTo(singletonList(PeriodeÅrsak.REFUSJON_OPPHØRER));
        List<BeregningsgrunnlagPrStatusOgAndel> andelerIAndrePeriode = perioder.get(1).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andelerIAndrePeriode).hasSize(1);
        assertThat(andelerIAndrePeriode.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    public void skalSetteRefusjonskravForSøktRefusjonFraEtterSkjæringstidspunktUtenOpphørsdato() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        var arbId = InternArbeidsforholdRef.namedRef("A");
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5), arbId,
            Arbeidsgiver.virksomhet(ORG_NUMMER));
        List<LocalDateInterval> berPerioder = singletonList(new LocalDateInterval(SKJÆRINGSTIDSPUNKT, null));
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningTestUtil.lagGjeldendeBeregningsgrunnlag(behandlingRef, SKJÆRINGSTIDSPUNKT,
            berPerioder);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1, null,
            SKJÆRINGSTIDSPUNKT.atStartOfDay());
        fjernOgLeggTilNyBeregningAktivitet(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5), arbeidsgiver, arbId);

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getBeregningsgrunnlagPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        List<BeregningsgrunnlagPrStatusOgAndel> andelerIFørstePeriode = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andelerIFørstePeriode).hasSize(1);
        assertThat(andelerIFørstePeriode.get(0).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null))
            .isEqualByComparingTo(refusjonskrav1.multiply(ANTALL_MÅNEDER_I_ÅR));
    }

    @Test
    public void skalTesteEndringIRefusjon() {
        // Arrange
        var behandlingReferanse = lagre(scenario);
        var arbId = InternArbeidsforholdRef.namedRef("A");
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        DatoIntervallEntitet arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5));
        List<String> orgnrs = List.of();
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(orgnrs, behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(40000);
        List<Refusjon> refusjonsListe = List.of(
            new Refusjon(BigDecimal.valueOf(20000), SKJÆRINGSTIDSPUNKT.plusMonths(3)),
            new Refusjon(BigDecimal.valueOf(10000), SKJÆRINGSTIDSPUNKT.plusMonths(6)));
        LocalDate refusjonOpphørerDato = SKJÆRINGSTIDSPUNKT.plusMonths(9).minusDays(1);
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT.minusDays(1),
            arbeidsperiode, arbId, Arbeidsgiver.virksomhet(ORG_NUMMER));
        var im1 = inntektsmeldingTestUtil.opprettInntektsmeldingMedEndringerIRefusjon(behandlingReferanse, ORG_NUMMER, arbId, SKJÆRINGSTIDSPUNKT, inntekt,
            inntekt, refusjonOpphørerDato, refusjonsListe, SKJÆRINGSTIDSPUNKT.atStartOfDay());
        fjernOgLeggTilNyBeregningAktivitet(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusYears(5),
            arbeidsgiver, arbId);
        beregningsgrunnlag.getBeregningsgrunnlagPerioder().forEach(periode -> {
            BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsperiodeFom(arbeidsperiode.getFomDato())
                .medArbeidsperiodeTom(arbeidsperiode.getTomDato())
                .medArbeidsforholdRef(arbId.getReferanse());

            BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(periode);
        });

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(4);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(3).minusDays(1));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusMonths(3), SKJÆRINGSTIDSPUNKT.plusMonths(6).minusDays(1),
            PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV);
        assertBeregningsgrunnlagPeriode(perioder.get(2), SKJÆRINGSTIDSPUNKT.plusMonths(6), refusjonOpphørerDato, PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV);
        assertBeregningsgrunnlagPeriode(perioder.get(3), refusjonOpphørerDato.plusDays(1), null, PeriodeÅrsak.REFUSJON_OPPHØRER);
        Map<LocalDate, BeregningsgrunnlagPrStatusOgAndel> andeler = perioder.stream()
            .collect(Collectors.toMap(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom, p -> p.getBeregningsgrunnlagPrStatusOgAndelList().get(0)));
        assertThat(andeler.get(perioder.get(0).getBeregningsgrunnlagPeriodeFom()).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
            .orElse(null))
                .isEqualByComparingTo(inntekt.multiply(ANTALL_MÅNEDER_I_ÅR));
        assertThat(andeler.get(perioder.get(1).getBeregningsgrunnlagPeriodeFom()).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
            .orElse(null)).isEqualByComparingTo(BigDecimal.valueOf(20000 * 12));
        assertThat(andeler.get(perioder.get(2).getBeregningsgrunnlagPeriodeFom()).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
            .orElse(null)).isEqualByComparingTo(BigDecimal.valueOf(10000 * 12));
        assertThat(andeler.get(perioder.get(3).getBeregningsgrunnlagPeriodeFom()).getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
            .orElse(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // Beregningsgrunnlag: En andel hos arbeidsgiver
    // Yrkesaktivitet har to ansettelsesperioder med to dagers mellomrom
    // Inntektsmelding: Inneholder orgnr, ingen arbId, inntekt = refusjon

    @Test
    public void skalIkkeLeggeTilArbeidsforholdSomTilkommerEtterSkjæringstidspunktDersomDetAlleredeEksisterer() {
        var scenario = TestScenarioBuilder.nyttScenario();
        BigDecimal inntekt = BigDecimal.valueOf(40000);
        DatoIntervallEntitet arbeidsperiode1 = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusMonths(1));
        DatoIntervallEntitet arbeidsperiode2 = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(3), TIDENES_ENDE);

        var aaBuilder1 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medPeriode(arbeidsperiode1);
        var aaBuilder2 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medPeriode(arbeidsperiode2);
        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aaBuilder1)
            .leggTilAktivitetsAvtale(aaBuilder2);
        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .leggTilYrkesaktivitet(yaBuilder)
            .medAktørId(scenario.getSøkerAktørId());
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);

        var behandlingReferanse = lagre(scenario);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, InternArbeidsforholdRef.nyRef(), SKJÆRINGSTIDSPUNKT, List.of(),
            inntekt.intValue(), inntekt.intValue(), SKJÆRINGSTIDSPUNKT.atStartOfDay());

        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        beregningsgrunnlag.getBeregningsgrunnlagPerioder().forEach(periode -> {
            BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold.builder()
                .medArbeidsperiodeFom(arbeidsperiode2.getFomDato())
                .medArbeidsperiodeTom(arbeidsperiode2.getTomDato())
                .medArbeidsgiver(arbeidsgiver);

            BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(periode);
        });

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList();
        assertThat(andeler).hasSize(1);
        Optional<BGAndelArbeidsforhold> bgaOpt = andeler.get(0).getBgAndelArbeidsforhold();
        assertThat(bgaOpt).hasValueSatisfying(bga -> {
            assertThat(bga.getArbeidsgiver()).isEqualTo(arbeidsgiver);
            assertThat(bga.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()).isFalse();
            assertThat(bga.getRefusjonskravPrÅr()).isEqualByComparingTo(inntekt.multiply(ANTALL_MÅNEDER_I_ÅR));
        });
    }

    @Test
    public void skalSplitteBeregningsgrunnlagOgLeggeTilNyAndelVedEndringssøknadNårSelvstendigNæringsdrivendeTilkommerOgGraderes() {
        // Arrange
        LocalDate graderingFom = SKJÆRINGSTIDSPUNKT.plusDays(10);
        LocalDate graderingTom = SKJÆRINGSTIDSPUNKT.plusDays(20);

        var scenario = TestScenarioBuilder.nyttScenario();

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, List.of(ORG_NUMMER, ORG_NUMMER_2));
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER, ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_2, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1,
            SKJÆRINGSTIDSPUNKT.atStartOfDay());

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag, aktivitetGradering, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(3);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(9));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusDays(10), graderingTom, PeriodeÅrsak.GRADERING);
        assertBeregningsgrunnlagPeriode(perioder.get(2), graderingTom.plusDays(1), null, PeriodeÅrsak.GRADERING_OPPHØRER);
        assertThat(perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        assertThat(perioder.get(1).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(3);
        assertAndelStatuser(perioder.get(1).getBeregningsgrunnlagPrStatusOgAndelList(),
            Arrays.asList(AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE));

    }

    @Test
    public void skalLeggeTilAndelSomTilkommerEtterSkjæringstidspunktForSøktGraderingUtenRefusjon() {
        // Arrange
        LocalDate graderingFom = SKJÆRINGSTIDSPUNKT.plusMonths(2);
        LocalDate graderingTom = SKJÆRINGSTIDSPUNKT.plusMonths(5);

        var scenario = TestScenarioBuilder.nyttScenario();

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        BigDecimal inntekt = BigDecimal.valueOf(40000);
        DatoIntervallEntitet arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, TIDENES_ENDE);

        var aaBuilder2 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medPeriode(arbeidsperiode);

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aaBuilder2);

        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .leggTilYrkesaktivitet(yaBuilder)
            .medAktørId(scenario.getSøkerAktørId());
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);

        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, singletonList(ORG_NUMMER_2));
        fjernAktivitet(arbeidsgiver, InternArbeidsforholdRef.nullRef());

        var behandlingReferanse = lagre(scenario);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_2, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, SKJÆRINGSTIDSPUNKT.atStartOfDay());

        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        beregningsgrunnlag.getBeregningsgrunnlagPerioder().forEach(periode -> {
            BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold.builder()
                .medArbeidsperiodeFom(arbeidsperiode.getFomDato())
                .medArbeidsperiodeTom(arbeidsperiode.getTomDato())
                .medArbeidsgiver(arbeidsgiver2);

            BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(periode);
        });

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag, aktivitetGradering, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode = perioder.get(0);
        assertThat(beregningsgrunnlagPeriode.getPeriodeÅrsaker()).isEmpty();
        assertThat(beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);

        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode2 = perioder.get(1);
        assertThat(beregningsgrunnlagPeriode2.getPeriodeÅrsaker()).containsExactly(PeriodeÅrsak.GRADERING);
        assertThat(beregningsgrunnlagPeriode2.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        Optional<BGAndelArbeidsforhold> baaOpt = finnBGAndelArbeidsforhold(beregningsgrunnlagPeriode2, arbeidsgiver.getIdentifikator());
        assertThat(baaOpt).as("BGAndelArbeidsforhold")
            .hasValueSatisfying(baa -> assertThat(baa.getRefusjonskravPrÅr()).as("RefusjonskravPrÅr").isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    public void skalLeggeTilAndelHvorBrukerErIPermisjonPåSkjæringstidspunktetOgSøkerRefusjon() {
        var scenario = TestScenarioBuilder.nyttScenario();
        InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        BigDecimal inntekt = BigDecimal.valueOf(40000);
        LocalDate permisjonFom = SKJÆRINGSTIDSPUNKT.minusMonths(1);
        LocalDate permisjonTom = SKJÆRINGSTIDSPUNKT.plusMonths(1);

        DatoIntervallEntitet arbeidsperiode1 = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE);

        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(scenario.getSøkerAktørId());
        Arbeidsgiver arbeidsgiver = leggTilYrkesaktivitet(arbeidsperiode1, aktørArbeidBuilder, ORG_NUMMER);
        fjernOgLeggTilNyBeregningAktivitet(arbeidsperiode1.getFomDato(), permisjonFom.minusDays(1), arbeidsgiver, InternArbeidsforholdRef.nullRef());
        aktiviteter.add(lagAktivitet(arbeidsperiode1.getFomDato(), arbeidsperiode1.getTomDato(), arbeidsgiver, arbeidsforholdRef));

        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);

        var behandlingReferanse = lagre(scenario);
        ArbeidsforholdInformasjonBuilder bekreftetPermisjon = bekreftetPermisjon(arbeidsgiver, arbeidsforholdRef,
            permisjonFom, permisjonTom);
        iayTjeneste.lagreArbeidsforhold(behandlingReferanse.getId(), behandlingReferanse.getAktørId(), bekreftetPermisjon);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, SKJÆRINGSTIDSPUNKT.atStartOfDay());

        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode = perioder.get(0);
        assertThat(beregningsgrunnlagPeriode.getPeriodeÅrsaker()).isEmpty();
        assertThat(beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList()).isEmpty();

        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode2 = perioder.get(1);
        assertThat(beregningsgrunnlagPeriode2.getPeriode().getFomDato()).isEqualTo(permisjonTom.plusDays(1));
        assertThat(beregningsgrunnlagPeriode2.getPeriodeÅrsaker()).containsExactly(PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV);
        assertThat(beregningsgrunnlagPeriode2.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        Optional<BGAndelArbeidsforhold> baaOpt = finnBGAndelArbeidsforhold(beregningsgrunnlagPeriode2, arbeidsgiver.getIdentifikator());
        assertThat(baaOpt).as("BGAndelArbeidsforhold")
            .hasValueSatisfying(baa -> assertThat(baa.getRefusjonskravPrÅr()).as("RefusjonskravPrÅr").isEqualByComparingTo(BigDecimal.valueOf(480_000)));
    }

    private ArbeidsforholdInformasjonBuilder bekreftetPermisjon(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, LocalDate fom, LocalDate tom) {
        ArbeidsforholdInformasjonBuilder arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = arbeidsforholdInformasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref);
        overstyringBuilder.medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        return arbeidsforholdInformasjonBuilder
            .leggTil(overstyringBuilder);
    }

    @Test
    public void skalLeggeTilAndelSomTilkommerPåSkjæringstidspunkt() {
        var scenario = TestScenarioBuilder.nyttScenario();
        BigDecimal inntekt = BigDecimal.valueOf(40000);
        DatoIntervallEntitet arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, TIDENES_ENDE);

        var aaBuilder2 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medPeriode(arbeidsperiode);

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aaBuilder2);

        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .leggTilYrkesaktivitet(yaBuilder)
            .medAktørId(scenario.getSøkerAktørId());
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);

        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, singletonList(ORG_NUMMER_2));
        fjernAktivitet(arbeidsgiver, InternArbeidsforholdRef.nullRef());

        var behandlingReferanse = lagre(scenario);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, SKJÆRINGSTIDSPUNKT.atStartOfDay());

        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        beregningsgrunnlag.getBeregningsgrunnlagPerioder().forEach(periode -> {
            BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold.builder()
                .medArbeidsperiodeFom(arbeidsperiode.getFomDato())
                .medArbeidsperiodeTom(arbeidsperiode.getTomDato())
                .medArbeidsgiver(arbeidsgiver2);

            BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(periode);
        });

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode = perioder.get(0);
        assertThat(beregningsgrunnlagPeriode.getPeriodeÅrsaker()).isEmpty();
        assertThat(beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        Optional<BGAndelArbeidsforhold> baaOpt = finnBGAndelArbeidsforhold(beregningsgrunnlagPeriode, arbeidsgiver.getIdentifikator());
        assertThat(baaOpt).as("BGAndelArbeidsforhold").hasValueSatisfying(
            baa -> assertThat(baa.getRefusjonskravPrÅr()).as("RefusjonskravPrÅr").isEqualByComparingTo(inntekt.multiply(ANTALL_MÅNEDER_I_ÅR)));
    }

    @Test
    public void skalLeggeTilAndelSomTilkommerPåSkjæringstidspunktOgSletteVedOpphør() {
        var scenario = TestScenarioBuilder.nyttScenario();
        BigDecimal inntekt = BigDecimal.valueOf(40000);
        DatoIntervallEntitet arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, TIDENES_ENDE);

        var aaBuilder2 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medPeriode(arbeidsperiode);

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aaBuilder2);

        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .leggTilYrkesaktivitet(yaBuilder)
            .medAktørId(scenario.getSøkerAktørId());
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);

        var behandlingReferanse = lagre(scenario);
        LocalDate refusjonOpphørerFom = SKJÆRINGSTIDSPUNKT.plusMonths(1);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, refusjonOpphørerFom,
            SKJÆRINGSTIDSPUNKT.atStartOfDay());

        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        beregningsgrunnlag.getBeregningsgrunnlagPerioder().forEach(periode -> {
            BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold.builder()
                .medArbeidsperiodeFom(arbeidsperiode.getFomDato())
                .medArbeidsperiodeTom(arbeidsperiode.getTomDato())
                .medArbeidsgiver(arbeidsgiver2);

            BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(periode);
        });

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag,
            AktivitetGradering.INGEN_GRADERING, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode = perioder.get(0);
        assertThat(beregningsgrunnlagPeriode.getPeriodeÅrsaker()).isEmpty();
        assertThat(beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        Optional<BGAndelArbeidsforhold> baaOpt = finnBGAndelArbeidsforhold(beregningsgrunnlagPeriode, arbeidsgiver.getIdentifikator());
        assertThat(baaOpt).hasValueSatisfying(baa -> assertThat(baa.getRefusjonskravPrÅr()).isEqualByComparingTo(inntekt.multiply(ANTALL_MÅNEDER_I_ÅR)));

        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode2 = perioder.get(1);
        assertThat(beregningsgrunnlagPeriode2.getPeriodeÅrsaker()).containsExactlyInAnyOrder(PeriodeÅrsak.REFUSJON_OPPHØRER);
        assertThat(beregningsgrunnlagPeriode2.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        baaOpt = finnBGAndelArbeidsforhold(beregningsgrunnlagPeriode2, arbeidsgiver.getIdentifikator());
        assertThat(baaOpt).isNotPresent();
    }

    @Test
    public void skalLeggeTilAndelSomTilkommerPåSkjæringstidspunktForSøktGraderingUtenRefusjon() {
        // Arrange
        LocalDate graderingFom = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingTom = SKJÆRINGSTIDSPUNKT.plusMonths(2);

        var scenario = TestScenarioBuilder.nyttScenario();

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        BigDecimal inntekt = BigDecimal.valueOf(40000);
        DatoIntervallEntitet arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, TIDENES_ENDE);

        var aaBuilder2 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medPeriode(arbeidsperiode);

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aaBuilder2);

        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .leggTilYrkesaktivitet(yaBuilder)
            .medAktørId(scenario.getSøkerAktørId());
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);

        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, singletonList(ORG_NUMMER_2));
        fjernAktivitet(arbeidsgiver, InternArbeidsforholdRef.nullRef());

        var behandlingReferanse = lagre(scenario);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_2, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, SKJÆRINGSTIDSPUNKT.atStartOfDay());

        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        beregningsgrunnlag.getBeregningsgrunnlagPerioder().forEach(periode -> {
            BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold.builder()
                .medArbeidsperiodeFom(arbeidsperiode.getFomDato())
                .medArbeidsperiodeTom(arbeidsperiode.getTomDato())
                .medArbeidsgiver(arbeidsgiver2);

            BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(periode);
        });

        // Act

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag, aktivitetGradering, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(1);
        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode = perioder.get(0);
        assertThat(beregningsgrunnlagPeriode.getPeriodeÅrsaker()).containsExactly(PeriodeÅrsak.GRADERING);
        assertThat(beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        Optional<BGAndelArbeidsforhold> baaOpt = finnBGAndelArbeidsforhold(beregningsgrunnlagPeriode, arbeidsgiver.getIdentifikator());
        assertThat(baaOpt).as("BGAndelArbeidsforhold")
            .hasValueSatisfying(baa -> assertThat(baa.getRefusjonskravPrÅr()).as("RefusjonskravPrÅr").isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    public void skalLeggeTilAndelSomTilkommerPåSkjæringstidspunktMedOpphørUtenSlettingPgaGradering() {
        // Arrange
        LocalDate graderingFom = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingTom = SKJÆRINGSTIDSPUNKT.plusMonths(2);

        var scenario = TestScenarioBuilder.nyttScenario();

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        BigDecimal inntekt = BigDecimal.valueOf(40000);
        DatoIntervallEntitet arbeidsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, TIDENES_ENDE);

        var aaBuilder2 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medPeriode(arbeidsperiode);

        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aaBuilder2);

        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .leggTilYrkesaktivitet(yaBuilder)
            .medAktørId(scenario.getSøkerAktørId());
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);

        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, singletonList(ORG_NUMMER_2));
        fjernAktivitet(arbeidsgiver, InternArbeidsforholdRef.nullRef());

        var behandlingReferanse = lagre(scenario);
        LocalDate refusjonOpphørerFom = SKJÆRINGSTIDSPUNKT.plusMonths(1);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, refusjonOpphørerFom,
            SKJÆRINGSTIDSPUNKT.atStartOfDay());

        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        beregningsgrunnlag.getBeregningsgrunnlagPerioder().forEach(periode -> {
            BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold.builder()
                .medArbeidsperiodeFom(arbeidsperiode.getFomDato())
                .medArbeidsperiodeTom(arbeidsperiode.getTomDato())
                .medArbeidsgiver(arbeidsgiver2);

            BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(periode);
        });
        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag, aktivitetGradering, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(2);
        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode = perioder.get(0);
        assertThat(beregningsgrunnlagPeriode.getPeriodeÅrsaker()).isEmpty();
        assertThat(beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        Optional<BGAndelArbeidsforhold> baaOpt = finnBGAndelArbeidsforhold(beregningsgrunnlagPeriode, arbeidsgiver.getIdentifikator());
        assertThat(baaOpt).hasValueSatisfying(baa -> assertThat(baa.getRefusjonskravPrÅr()).isEqualByComparingTo(inntekt.multiply(ANTALL_MÅNEDER_I_ÅR)));

        BeregningsgrunnlagPeriode beregningsgrunnlagPeriode2 = perioder.get(1);
        assertThat(beregningsgrunnlagPeriode2.getPeriodeÅrsaker()).containsExactly(PeriodeÅrsak.REFUSJON_OPPHØRER);
        assertThat(beregningsgrunnlagPeriode2.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
    }

    @Test
    public void skalKasteFeilHvisAntallPerioderErMerEnn1() {
        // Arrange
        var scenario = TestScenarioBuilder.nyttScenario();
        BeregningsgrunnlagPeriode.Builder periode1 = lagBeregningsgrunnlagPerioderBuilder(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT, List.of(ORG_NUMMER));
        BeregningsgrunnlagPeriode.Builder periode2 = lagBeregningsgrunnlagPerioderBuilder(SKJÆRINGSTIDSPUNKT.plusDays(1), null, List.of(ORG_NUMMER));
        BeregningsgrunnlagEntitet beregningsgrunnlag = scenario.medBeregningsgrunnlag()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(GRUNNBELØP)
            .leggTilBeregningsgrunnlagPeriode(periode1)
            .leggTilBeregningsgrunnlagPeriode(periode2)
            .build();
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        // Act
        var input = new BeregningsgrunnlagInput(behandlingRef, null, null, AktivitetGradering.INGEN_GRADERING, null)
            .medBeregningsgrunnlagGrunnlag(grunnlag);

        Assert.assertThrows(TekniskException.class, () -> {
            tjeneste.fastsettPerioderForNaturalytelse(input, beregningsgrunnlag);
        });

    }

    @Test
    public void lagPeriodeForGraderingOver6G() {
        // Arrange
        LocalDate graderingFom = SKJÆRINGSTIDSPUNKT.plusWeeks(9);
        LocalDate graderingTom = SKJÆRINGSTIDSPUNKT.plusWeeks(18).minusDays(1);

        var scenario = TestScenarioBuilder.nyttScenario();

        var arbeidsgiverGradering = Arbeidsgiver.virksomhet(ORG_NUMMER);

        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, List.of(ORG_NUMMER, ORG_NUMMER_2));
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER, ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_2, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1,
            SKJÆRINGSTIDSPUNKT.atStartOfDay());

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medArbeidsgiver(arbeidsgiverGradering)
            .medStatus(AktivitetStatus.ARBEIDSTAKER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag, aktivitetGradering, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(3);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusWeeks(9).minusDays(1));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusWeeks(9), graderingTom, PeriodeÅrsak.GRADERING);
        assertBeregningsgrunnlagPeriode(perioder.get(2), graderingTom.plusDays(1), null, PeriodeÅrsak.GRADERING_OPPHØRER);
    }

    @Test
    public void skalSplitteBeregningsgrunnlagOgLeggeTilNyAndelVedEndringssøknadNårFrilansTilkommerOgGraderes() {
        // Arrange
        LocalDate graderingFom = SKJÆRINGSTIDSPUNKT.plusDays(10);
        LocalDate graderingTom = SKJÆRINGSTIDSPUNKT.plusDays(20);

        var scenario = TestScenarioBuilder.nyttScenario();

        leggTilYrkesaktiviteterOgBeregningAktiviteter(scenario, List.of(ORG_NUMMER, ORG_NUMMER_2));
        var behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlagMedOverstyring(List.of(ORG_NUMMER, ORG_NUMMER_2), behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();

        BigDecimal inntekt1 = BigDecimal.valueOf(90000);
        BigDecimal refusjonskrav1 = inntekt1;
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORG_NUMMER_2, SKJÆRINGSTIDSPUNKT, refusjonskrav1, inntekt1,
            SKJÆRINGSTIDSPUNKT.atStartOfDay());

        AktivitetGradering aktivitetGradering = new AktivitetGradering(AndelGradering.builder()
            .medStatus(AktivitetStatus.FRILANSER)
            .medGradering(graderingFom, graderingTom, 50)
            .build());

        // Act
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = fastsettPerioderForRefusjonOgGradering(behandlingRef, grunnlag, beregningsgrunnlag, aktivitetGradering, List.of(im1));

        // Assert
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        assertThat(perioder).hasSize(3);
        assertBeregningsgrunnlagPeriode(perioder.get(0), SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(9));
        assertBeregningsgrunnlagPeriode(perioder.get(1), SKJÆRINGSTIDSPUNKT.plusDays(10), graderingTom, PeriodeÅrsak.GRADERING);
        assertThat(perioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);
        assertThat(perioder.get(1).getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(3);
        assertAndelStatuser(perioder.get(1).getBeregningsgrunnlagPrStatusOgAndelList(),
            Arrays.asList(AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.FRILANSER));
        assertBeregningsgrunnlagPeriode(perioder.get(2), graderingTom.plusDays(1), null, PeriodeÅrsak.GRADERING_OPPHØRER);

    }

    private BeregningAktivitetOverstyringEntitet lagOverstyringForAktivitet(InternArbeidsforholdRef arbId, Arbeidsgiver arbeidsgiver, BeregningAktivitetHandlingType handlingIkkeBenytt) {
        return BeregningAktivitetOverstyringEntitet.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(arbId)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSPERIODE.getFomDato(), ARBEIDSPERIODE.getTomDato()))
            .medHandling(handlingIkkeBenytt).build();
    }

    private void assertAndelStatuser(List<BeregningsgrunnlagPrStatusOgAndel> andeler, List<AktivitetStatus> statuser) {
        List<AktivitetStatus> aktivitetStatuser = andeler.stream().map(BeregningsgrunnlagPrStatusOgAndel::getAktivitetStatus).collect(Collectors.toList());
        assertThat(aktivitetStatuser).containsAll(statuser);

    }

    private Optional<BGAndelArbeidsforhold> finnBGAndelArbeidsforhold(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, String orgnr) {
        return beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .flatMap(andel -> andel.getBgAndelArbeidsforhold().stream())
            .filter(bga -> bga.getArbeidsforholdOrgnr().equals(orgnr))
            .findFirst();
    }

    private void assertBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, LocalDate expectedFom, LocalDate expectedTom,
                                                 PeriodeÅrsak... perioderÅrsaker) {
        assertThat(beregningsgrunnlagPeriode.getBeregningsgrunnlagPeriodeFom()).as("fom").isEqualTo(expectedFom);
        assertThat(beregningsgrunnlagPeriode.getBeregningsgrunnlagPeriodeTom()).as("tom").isEqualTo(expectedTom);
        assertThat(beregningsgrunnlagPeriode.getPeriodeÅrsaker()).as("periodeÅrsaker").containsExactlyInAnyOrder(perioderÅrsaker);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlagMedOverstyring(List<String> orgnrs, BehandlingReferanse behandlingReferanse,
                                                                                  BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        return lagBeregningsgrunnlag(orgnrs, behandlingReferanse, beregningAktivitetAggregat, null);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlagMedSaksbehandlet(List<String> orgnrs, BehandlingReferanse behandlingReferanse,
                                                                                    BeregningAktivitetAggregatEntitet beregningAktivitetAggregat, BeregningAktivitetAggregatEntitet saksbehandlet) {
        BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder = lagBeregningsgrunnlagPerioderBuilder(SKJÆRINGSTIDSPUNKT, null, orgnrs);
        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(GRUNNBELØP)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).medHjemmel(Hjemmel.F_14_7));
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeBuilder);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagBuilder.build();
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(bg)
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .medSaksbehandletAktiviteter(saksbehandlet)
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.FORESLÅTT);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlag(List<String> orgnrs, BehandlingReferanse behandlingReferanse,
                                                                    BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                    BeregningAktivitetOverstyringerEntitet beregningAktivitetOverstyringerEntitet) {
        BeregningsgrunnlagPeriode.Builder beregningsgrunnlagPeriodeBuilder = lagBeregningsgrunnlagPerioderBuilder(SKJÆRINGSTIDSPUNKT, null, orgnrs);
        BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(GRUNNBELØP)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).medHjemmel(Hjemmel.F_14_7));
        beregningsgrunnlagBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeBuilder);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagBuilder.build();
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(bg)
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .medOverstyring(beregningAktivitetOverstyringerEntitet)
            .build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.FORESLÅTT);
    }

    private BeregningsgrunnlagPeriode.Builder lagBeregningsgrunnlagPerioderBuilder(LocalDate fom, LocalDate tom, List<String> orgnrs) {
        BeregningsgrunnlagPeriode.Builder builder = BeregningsgrunnlagPeriode.builder();
        for (String orgnr : orgnrs) {
            Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
            BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                    .medArbeidsgiver(arbeidsgiver)
                    .medArbeidsperiodeFom(SKJÆRINGSTIDSPUNKT.minusYears(1)));
            builder.leggTilBeregningsgrunnlagPrStatusOgAndel(andelBuilder);
        }
        return builder
            .medBeregningsgrunnlagPeriode(fom, tom);
    }

}
