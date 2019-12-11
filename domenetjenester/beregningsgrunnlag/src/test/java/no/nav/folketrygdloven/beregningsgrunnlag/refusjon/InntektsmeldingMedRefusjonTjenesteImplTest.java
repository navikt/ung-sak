package no.nav.folketrygdloven.beregningsgrunnlag.refusjon;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningInntektsmeldingTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.vedtak.felles.jpa.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingMedRefusjonTjenesteImplTest {
    private static final String ORGNR = "974760673";
    private static final String ORGNR2 = "915933149";

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(entityManager);
    private InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private BeregningInntektsmeldingTestUtil inntektsmeldingTestUtil;
    private Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
        .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT)
        .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
        .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
        .build();

    @Before
    public void oppsett() {
        var iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
        inntektsmeldingMedRefusjonTjeneste = new InntektsmeldingMedRefusjonTjeneste(inntektsmeldingTjeneste);
        inntektsmeldingTestUtil = new BeregningInntektsmeldingTestUtil(inntektsmeldingTjeneste);
    }

    @Test
    public void skal_finne_arbeidsgivere_som_har_søkt_for_sent() {
        // Arrange

        AbstractTestScenario<?> scenario = TestScenarioBuilder.nyttScenario();
        BeregningAktivitetAggregatEntitet aktivitetAggregat = leggTilAktivitet(scenario, List.of(ORGNR, ORGNR2));
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet(ORGNR2);
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR, SKJÆRINGSTIDSPUNKT, BigDecimal.TEN, BigDecimal.TEN, SKJÆRINGSTIDSPUNKT.plusMonths(3).atStartOfDay());
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR2, SKJÆRINGSTIDSPUNKT, BigDecimal.TEN, BigDecimal.TEN, SKJÆRINGSTIDSPUNKT.plusMonths(2).atStartOfDay());
        BeregningsgrunnlagGrunnlagEntitet grunnlag = byggGrunnlag(aktivitetAggregat, List.of(arbeidsgiver, arbeidsgiver2), behandlingReferanse);

        // Act
        Set<Arbeidsgiver> arbeidsgivereSomHarSøktForSent = inntektsmeldingMedRefusjonTjeneste.finnArbeidsgiverSomHarSøktRefusjonForSent(behandlingReferanse, iayTjeneste.hentGrunnlag(behandlingReferanse.getId()), grunnlag);

        // Assert
        assertThat(arbeidsgivereSomHarSøktForSent.size()).isEqualTo(1);
        assertThat(arbeidsgivereSomHarSøktForSent.iterator().next()).isEqualTo(arbeidsgiver);
    }


    @Test
    public void skal_returnere_tomt_set_om_ingen_inntektsmeldinger_er_mottatt() {
        // Arrange
        AbstractTestScenario<?> scenario = TestScenarioBuilder.nyttScenario();
        BeregningAktivitetAggregatEntitet aktivitetAggregat = leggTilAktivitet(scenario, List.of(ORGNR, ORGNR2));
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet(ORGNR2);
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = byggGrunnlag(aktivitetAggregat, List.of(arbeidsgiver, arbeidsgiver2), behandlingReferanse);

        // Act
        Set<Arbeidsgiver> arbeidsgivereSomHarSøktForSent = inntektsmeldingMedRefusjonTjeneste.finnArbeidsgiverSomHarSøktRefusjonForSent(behandlingReferanse, iayTjeneste.hentGrunnlag(behandlingReferanse.getId()), grunnlag);

        // Assert
        assertThat(arbeidsgivereSomHarSøktForSent.size()).isEqualTo(0);
    }

    @Test
    public void skal_returnere_tomt_set_om_ingen_inntektsmeldinger_er_mottatt_for_sent() {
        // Arrange
        AbstractTestScenario<?> scenario = TestScenarioBuilder.nyttScenario();
        BeregningAktivitetAggregatEntitet aktivitetAggregat = leggTilAktivitet(scenario, List.of(ORGNR, ORGNR2));
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet(ORGNR2);
        BehandlingReferanse behandlingReferanse = lagre(scenario);
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR, SKJÆRINGSTIDSPUNKT, BigDecimal.TEN, BigDecimal.TEN, SKJÆRINGSTIDSPUNKT.plusMonths(1).atStartOfDay());
        inntektsmeldingTestUtil.opprettInntektsmelding(behandlingReferanse, ORGNR2, SKJÆRINGSTIDSPUNKT, BigDecimal.TEN, BigDecimal.TEN, SKJÆRINGSTIDSPUNKT.plusMonths(2).atStartOfDay());
        BeregningsgrunnlagGrunnlagEntitet grunnlag = byggGrunnlag(aktivitetAggregat, List.of(arbeidsgiver, arbeidsgiver2), behandlingReferanse);

        // Act
        Set<Arbeidsgiver> arbeidsgivereSomHarSøktForSent = inntektsmeldingMedRefusjonTjeneste.finnArbeidsgiverSomHarSøktRefusjonForSent(behandlingReferanse, iayTjeneste.hentGrunnlag(behandlingReferanse.getId()), grunnlag);

        // Assert
        assertThat(arbeidsgivereSomHarSøktForSent.size()).isEqualTo(0);
    }


    private BeregningsgrunnlagGrunnlagEntitet byggGrunnlag(BeregningAktivitetAggregatEntitet aktivitetAggregat, List<Arbeidsgiver> arbeidsgivere, BehandlingReferanse behandlingReferanse) {
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(aktivitetAggregat)
            .medBeregningsgrunnlag(lagBeregningsgrunnlag(arbeidsgivere)).build(behandlingReferanse.getId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag(List<Arbeidsgiver> ags) {

        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)).build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(bg);
        ags.forEach(ag -> BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ag))
            .build(periode)
        );
        return bg;
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening).medSkjæringstidspunkt(skjæringstidspunkt);
    }

    private BeregningAktivitetAggregatEntitet leggTilAktivitet(AbstractTestScenario<?> scenario, List<String> orgnr) {
        DatoIntervallEntitet arbeidsperiode1 = DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), Tid.TIDENES_ENDE);
        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(scenario.getSøkerAktørId());
        BeregningAktivitetAggregatEntitet.Builder aktivitetAggregatBuilder = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT);
        for (String nr : orgnr) {
            Arbeidsgiver ag = leggTilYrkesaktivitet(arbeidsperiode1, aktørArbeidBuilder, nr);
            aktivitetAggregatBuilder.leggTilAktivitet(lagAktivitet(arbeidsperiode1, ag));
        }
        scenario.getInntektArbeidYtelseScenarioTestBuilder().getKladd().leggTilAktørArbeid(aktørArbeidBuilder);
        return aktivitetAggregatBuilder.build();
    }

    private BeregningAktivitetEntitet lagAktivitet(DatoIntervallEntitet arbeidsperiode1, Arbeidsgiver ag) {
        return BeregningAktivitetEntitet.builder().medArbeidsgiver(ag).medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID).medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(arbeidsperiode1.getFomDato(), arbeidsperiode1.getTomDato())).build();
    }

    private Arbeidsgiver leggTilYrkesaktivitet(DatoIntervallEntitet arbeidsperiode, InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder, String orgnr) {
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

}
