package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.GrunnbeløpTestKonstanter;
import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.refusjon.InntektsmeldingMedRefusjonTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatusV2;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.ArbeidsforholdOgInntektsmelding;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.BruttoBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Gradering;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.PeriodeModell;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.PeriodisertBruttoBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Refusjonskrav;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningInntektsmeldingTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;

public class MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2018, Month.JANUARY, 9);
    private static final String ORGNR = "984661185";
    private static final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
        .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
        .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
        .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT)
        .build();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private final RepositoryProvider repositoryProvider = new RepositoryProvider(entityManager);

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);

    private BeregningInntektsmeldingTestUtil inntektsmeldingTestUtil = new BeregningInntektsmeldingTestUtil(inntektsmeldingTjeneste);
    private MapFastsettBeregningsgrunnlagPerioderFraVLTilRegel mapperRefusjonGradering;

    @Before
    public void setup() {
        InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste = new InntektsmeldingMedRefusjonTjeneste(inntektsmeldingTjeneste);

        mapperRefusjonGradering = new MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering(inntektsmeldingMedRefusjonTjeneste);
    }

    @Test
    public void map_utenGraderingEllerRefusjon() {
        // Arrange
        var scenario = TestScenarioBuilder.nyttScenario();
        leggTilYrkesaktiviteter(scenario, List.of(ORGNR));
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        BehandlingReferanse referanse = lagre(scenario);
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = lagBeregningAktiviteter(SKJÆRINGSTIDSPUNKT, arbeidsgiver);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag(arbeidsgiver, referanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(referanse.getId())).build();
        var input = new BeregningsgrunnlagInput(referanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null);

        // Act
        PeriodeModell regelmodell = mapRefusjonGradering(input.medBeregningsgrunnlagGrunnlag(grunnlag), beregningsgrunnlag);

        // Assert
        assertUtenRefusjonOgGradering(regelmodell);
    }

    @Test
    public void mapRefusjonOgGradering_utenGraderingEllerRefusjon() {
        // Arrange
        var scenario = TestScenarioBuilder.nyttScenario();
        leggTilYrkesaktiviteter(scenario, List.of(ORGNR));
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        var behandlingReferanse = lagre(scenario);
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = lagBeregningAktiviteter(SKJÆRINGSTIDSPUNKT, arbeidsgiver);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag(arbeidsgiver, behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).build();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null);

        PeriodeModell regelmodell = mapRefusjonGradering(input.medBeregningsgrunnlagGrunnlag(grunnlag), beregningsgrunnlag);

        // Assert
        assertUtenRefusjonOgGradering(regelmodell);
        List<PeriodisertBruttoBeregningsgrunnlag> periodisertBruttoBeregningsgrunnlagList = regelmodell.getPeriodisertBruttoBeregningsgrunnlagList();
        assertThat(periodisertBruttoBeregningsgrunnlagList).hasSize(1);
        assertThat(periodisertBruttoBeregningsgrunnlagList.get(0).getPeriode()).isEqualTo(Periode.of(SKJÆRINGSTIDSPUNKT, Tid.TIDENES_ENDE));
        List<BruttoBeregningsgrunnlag> bruttoBeregningsgrunnlagList = periodisertBruttoBeregningsgrunnlagList.get(0).getBruttoBeregningsgrunnlag();
        assertThat(bruttoBeregningsgrunnlagList).hasSize(1);
        BruttoBeregningsgrunnlag bruttoBeregningsgrunnlag = bruttoBeregningsgrunnlagList.get(0);
        assertThat(bruttoBeregningsgrunnlag.getAktivitetStatus()).isEqualTo(AktivitetStatusV2.AT);
        assertThat(bruttoBeregningsgrunnlag.getBruttoBeregningsgrunnlag()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(bruttoBeregningsgrunnlag.getArbeidsforhold().getOrgnr()).isEqualTo(ORGNR);
        assertThat(bruttoBeregningsgrunnlag.getArbeidsforhold().getArbeidsforholdId()).isNull();
    }

    @Test
    public void testMedRefusjon() {
        // Arrange
        var scenario = TestScenarioBuilder.nyttScenario();
        leggTilYrkesaktiviteter(scenario, List.of(ORGNR));
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        var behandlingReferanse = lagre(scenario);

        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = lagBeregningAktiviteter(SKJÆRINGSTIDSPUNKT, arbeidsgiver);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag(arbeidsgiver, behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(20000);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, SKJÆRINGSTIDSPUNKT.atStartOfDay());
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(im1).build();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null);

        // Act
        PeriodeModell regelmodell = mapRefusjonGradering(input.medBeregningsgrunnlagGrunnlag(grunnlag), beregningsgrunnlag);

        // Assert
        assertThat(regelmodell.getArbeidsforholdOgInntektsmeldinger()).hasSize(1);
        ArbeidsforholdOgInntektsmelding arbeidsforhold = regelmodell.getArbeidsforholdOgInntektsmeldinger().get(0);
        assertThat(arbeidsforhold.getRefusjoner()).hasSize(1);
        Refusjonskrav refusjonskrav = arbeidsforhold.getRefusjoner().get(0);
        assertThat(refusjonskrav.getFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(refusjonskrav.getMånedsbeløp()).isEqualByComparingTo(inntekt);
        assertThat(refusjonskrav.getPeriode().getTom()).isEqualTo(Tid.TIDENES_ENDE);
        assertThat(arbeidsforhold.getGyldigeRefusjonskrav()).isEmpty();
    }

    @Test
    public void refusjon_med_fleire_ya_før_skjæringstidspunktet_i_samme_org() {
        // Arrange
        var scenario = TestScenarioBuilder.nyttScenario();
        leggTilYrkesaktiviteter(scenario, List.of(ORGNR, ORGNR, ORGNR),
            List.of(
                DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.minusYears(1)),
                DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1).plusDays(1), SKJÆRINGSTIDSPUNKT.minusMonths(5)),
                DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(5).plusDays(1), Tid.TIDENES_ENDE)));
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        var behandlingReferanse = lagre(scenario);

        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = lagBeregningAktiviteter(SKJÆRINGSTIDSPUNKT, arbeidsgiver);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag(arbeidsgiver, behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();
        BigDecimal inntekt = BigDecimal.valueOf(60000);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, SKJÆRINGSTIDSPUNKT.atStartOfDay());

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(im1).build();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null);

        // Act
        PeriodeModell regelmodell = mapRefusjonGradering(input.medBeregningsgrunnlagGrunnlag(grunnlag), beregningsgrunnlag);

        // Assert
        assertThat(regelmodell.getArbeidsforholdOgInntektsmeldinger()).hasSize(1);
        ArbeidsforholdOgInntektsmelding arbeidsforhold = regelmodell.getArbeidsforholdOgInntektsmeldinger().get(0);
        assertThat(arbeidsforhold.getRefusjoner()).hasSize(1);
        Refusjonskrav refusjonskrav = arbeidsforhold.getRefusjoner().get(0);
        assertThat(refusjonskrav.getFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(refusjonskrav.getMånedsbeløp()).isEqualByComparingTo(inntekt);
        assertThat(refusjonskrav.getPeriode().getTom()).isEqualTo(Tid.TIDENES_ENDE);
        assertThat(arbeidsforhold.getGyldigeRefusjonskrav()).isEmpty();
    }

    @Test
    public void testMedGradering() {
        // Arrange
        LocalDate fom = SKJÆRINGSTIDSPUNKT.plusWeeks(9);
        LocalDate tom = SKJÆRINGSTIDSPUNKT.plusWeeks(18).minusDays(1);
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        var scenario = TestScenarioBuilder.nyttScenario();

        leggTilYrkesaktiviteter(scenario, List.of(ORGNR));
        var behandlingReferanse = lagre(scenario);

        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = lagBeregningAktiviteter(SKJÆRINGSTIDSPUNKT, arbeidsgiver);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag(arbeidsgiver, behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();

        var aktivitetGradering = new AktivitetGradering(List.of(
            AndelGradering.builder().medStatus(AktivitetStatus.ARBEIDSTAKER)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(new OrgNummer(ORGNR)))
                .leggTilGradering(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), BigDecimal.valueOf(50))
                .build()));

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).build();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, null, aktivitetGradering, null);

        PeriodeModell regelmodell = mapRefusjonGradering(input.medBeregningsgrunnlagGrunnlag(grunnlag), beregningsgrunnlag);

        // Assert
        ArbeidsforholdOgInntektsmelding arbeidsforhold = regelmodell.getArbeidsforholdOgInntektsmeldinger().get(0);
        assertThat(arbeidsforhold.getGraderinger()).hasSize(1);
        Gradering gradering = arbeidsforhold.getGraderinger().get(0);
        assertThat(gradering.getFom()).isEqualTo(fom);
        assertThat(gradering.getTom()).isEqualTo(tom);
    }

    @Test
    public void testToArbeidsforholdISammeVirksomhetEtTilkommerEtterSkjæringstidspunkt() {
        // Arrange
        var scenario = TestScenarioBuilder.nyttScenario();
        DatoIntervallEntitet arbeidsperiode1 = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE);
        DatoIntervallEntitet arbeidsperiode2 = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(2), Tid.TIDENES_ENDE);

        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(scenario.getSøkerAktørId());
        leggTilYrkesaktivitet(arbeidsperiode1, aktørArbeidBuilder, ORGNR);
        leggTilYrkesaktivitet(arbeidsperiode2, aktørArbeidBuilder, ORGNR);
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        var behandlingReferanse = lagre(scenario);
        BigDecimal inntekt = BigDecimal.valueOf(20000);
        var im1 = inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR, SKJÆRINGSTIDSPUNKT, inntekt, inntekt, SKJÆRINGSTIDSPUNKT.atStartOfDay());

        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = lagBeregningAktiviteter(SKJÆRINGSTIDSPUNKT, arbeidsgiver);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag(arbeidsgiver, behandlingReferanse, beregningAktivitetAggregat);
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().get();

        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.hentGrunnlag(behandlingReferanse.getId())).medInntektsmeldinger(im1).build();
        var input = new BeregningsgrunnlagInput(behandlingReferanse, iayGrunnlag, null, AktivitetGradering.INGEN_GRADERING, null);

        // Act
        PeriodeModell regelmodell = mapRefusjonGradering(input.medBeregningsgrunnlagGrunnlag(grunnlag), beregningsgrunnlag);

        // Assert
        List<ArbeidsforholdOgInntektsmelding> arbeidsforholdOgInntektsmeldinger = regelmodell.getArbeidsforholdOgInntektsmeldinger();
        assertThat(arbeidsforholdOgInntektsmeldinger).hasSize(1);
        ArbeidsforholdOgInntektsmelding arbeidsforhold = arbeidsforholdOgInntektsmeldinger.get(0);
        assertThat(arbeidsforhold.getStartdatoPermisjon()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(arbeidsforhold.getAndelsnr()).isEqualTo(1L);
        assertThat(arbeidsforhold.getRefusjoner()).hasSize(1);
        assertThat(arbeidsforhold.getRefusjoner().get(0).getMånedsbeløp()).isEqualByComparingTo(inntekt);
    }

    private PeriodeModell mapRefusjonGradering(BeregningsgrunnlagInput input, BeregningsgrunnlagEntitet beregningsgrunnlag) {
        PeriodeModell regelmodell = mapperRefusjonGradering.map(input, beregningsgrunnlag);
        return regelmodell;
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening).medSkjæringstidspunkt(skjæringstidspunkt);
    }

    private void assertUtenRefusjonOgGradering(PeriodeModell regelmodell) {
        assertThat(regelmodell.getSkjæringstidspunkt()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(regelmodell.getGrunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(GrunnbeløpTestKonstanter.GRUNNBELØP_2018));
        assertThat(regelmodell.getEksisterendePerioder()).hasSize(1);
        assertThat(regelmodell.getAndelGraderinger()).isEmpty();
        assertThat(regelmodell.getArbeidsforholdOgInntektsmeldinger()).hasSize(1);
        ArbeidsforholdOgInntektsmelding arbeidsforhold = regelmodell.getArbeidsforholdOgInntektsmeldinger().get(0);
        assertThat(arbeidsforhold.getRefusjoner()).isEmpty();
        assertThat(arbeidsforhold.getGyldigeRefusjonskrav()).isEmpty();
        assertThat(arbeidsforhold.getInnsendingsdatoFørsteInntektsmeldingMedRefusjon()).isNull();
        assertThat(arbeidsforhold.getAndelsnr()).isEqualTo(1L);
        assertThat(arbeidsforhold.getStartdatoPermisjon()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(arbeidsforhold.getNaturalYtelser()).isEmpty();
        assertThat(arbeidsforhold.getGraderinger()).isEmpty();
        assertThat(arbeidsforhold.getAnsettelsesperiode()).isEqualTo(Periode.of(SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE));
        assertThat(arbeidsforhold.getArbeidsforhold().getOrgnr()).isEqualTo(ORGNR);
        assertThat(arbeidsforhold.getAktivitetStatus()).isEqualTo(AktivitetStatusV2.AT);
    }

    private void leggTilYrkesaktiviteter(AbstractTestScenario<?> scenario, List<String> orgnrs) {
        DatoIntervallEntitet arbeidsperiode1 = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE);

        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(scenario.getSøkerAktørId());
        for (String orgnr : orgnrs) {
            leggTilYrkesaktivitet(arbeidsperiode1, aktørArbeidBuilder, orgnr);
        }
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);
    }

    private void leggTilYrkesaktiviteter(AbstractTestScenario<?> scenario, List<String> orgnrs, List<DatoIntervallEntitet> perioder) {
        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(scenario.getSøkerAktørId());
        for (int i = 0; i < orgnrs.size(); i++) {
            leggTilYrkesaktivitet(perioder.get(i), aktørArbeidBuilder, orgnrs.get(i));
        }
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);
    }

    private static Yrkesaktivitet leggTilYrkesaktivitet(DatoIntervallEntitet arbeidsperiode,
                                                        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder,
                                                        String orgnr) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        AktivitetsAvtaleBuilder aaBuilder1 = AktivitetsAvtaleBuilder.ny()
            .medProsentsats(BigDecimal.ZERO)
            .medPeriode(arbeidsperiode);
        YrkesaktivitetBuilder yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .leggTilAktivitetsAvtale(aaBuilder1);
        aktørArbeidBuilder.leggTilYrkesaktivitet(yaBuilder);
        return yaBuilder.build();
    }

    private BeregningAktivitetAggregatEntitet lagBeregningAktiviteter(LocalDate skjæringstidspunkt, Arbeidsgiver arbeidsgiver) {
        BeregningAktivitetAggregatEntitet.Builder builder = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(skjæringstidspunkt);
        BeregningAktivitetEntitet beregningAktivitet = BeregningAktivitetEntitet.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, Tid.TIDENES_ENDE))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver)
            .build();
        builder.leggTilAktivitet(beregningAktivitet);
        return builder.build();
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlag(Arbeidsgiver arbeidsgiver, BehandlingReferanse referanse,
                                                                    BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(BigDecimal.valueOf(GrunnbeløpTestKonstanter.GRUNNBELØP_2018))
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, Tid.TIDENES_ENDE)
                .leggTilBeregningsgrunnlagPrStatusOgAndel(
                    BeregningsgrunnlagPrStatusOgAndel.builder()
                        .medAndelsnr(1L)
                        .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                        .medBeregnetPrÅr(BigDecimal.TEN)
                        .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                            .medArbeidsgiver(arbeidsgiver))))
            .build();
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .medBeregningsgrunnlag(bg).build(referanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }
}
