package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.util.BeregningsgrunnlagTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;

public class AutopunktUtlederFastsettBeregningsaktiviteterTjenesteTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(entityManager);
    private BehandlingReferanse ref;
    private TestScenarioBuilder scenario;
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    // Test utils
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);
    private BeregningsgrunnlagTestUtil beregningsgrunnlagTestUtil = new BeregningsgrunnlagTestUtil(beregningsgrunnlagRepository, iayTjeneste);
    private BeregningIAYTestUtil iayTestUtil = new BeregningIAYTestUtil(iayTjeneste);

    @Before
    public void setUp() {
        scenario = TestScenarioBuilder.nyttScenario();
        scenario.medDefaultInntektArbeidYtelse();
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }

    @Test
    public void skal_vente_på_meldekort_når_har_AAP_og_meldekort_uten_AAP_status() {
        // Arrange
        LocalDate dagensdato = LocalDate.of(2019, 1, 4);
        LocalDate skjæringstidspunktOpptjening = LocalDate.of(2019, 2, 1);
        ref = lagReferanse(skjæringstidspunktOpptjening);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagTestUtil.lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, AktivitetStatus.ARBEIDSTAKER);
        LocalDate fom = LocalDate.of(2018, 9, 1);
        LocalDate tom = LocalDate.of(2019, 5, 1);
        LocalDate meldekortFom = LocalDate.of(2018, 12, 1);
        iayTestUtil.leggTilAktørytelse(ref, fom, tom, RelatertYtelseTilstand.LØPENDE, "123", FagsakYtelseType.ARBEIDSAVKLARINGSPENGER,
            Collections.emptyList(), Arbeidskategori.ARBEIDSTAKER, lagMeldekortPeriode(meldekortFom));

        // Act
        Optional<LocalDate> resultat = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(getAktørYtelse(), bg, dagensdato);

        //Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(skjæringstidspunktOpptjening.plusDays(1));
    }

    private Optional<AktørYtelse> getAktørYtelse() {
        return iayTjeneste.finnGrunnlag(ref.getBehandlingId()).flatMap(gr -> gr.getAktørYtelseFraRegister(ref.getAktørId()));
    }

    @Test
    public void skal_vente_på_meldekort_når_har_AAP_og_meldekort_uten_AAP_status_grenseverdi() {
        // Arrange
        LocalDate dagensdato = LocalDate.of(2019, 1, 4);
        LocalDate skjæringstidspunktOpptjening = LocalDate.of(2019, 2, 1);
        ref = lagReferanse(skjæringstidspunktOpptjening);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagTestUtil.lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, AktivitetStatus.ARBEIDSTAKER);
        LocalDate fom = LocalDate.of(2018, 9, 1);
        LocalDate tom = skjæringstidspunktOpptjening.minusDays(1);
        LocalDate meldekortFom = LocalDate.of(2018, 10, 1);
        iayTestUtil.leggTilAktørytelse(ref, fom, tom, RelatertYtelseTilstand.LØPENDE, "123", FagsakYtelseType.ARBEIDSAVKLARINGSPENGER,
            Collections.emptyList(), Arbeidskategori.ARBEIDSTAKER, lagMeldekortPeriode(meldekortFom));

        // Act
        Optional<LocalDate> resultat = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(getAktørYtelse(), bg, dagensdato);

        //Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(skjæringstidspunktOpptjening.plusDays(1));
    }

    private BehandlingReferanse lagReferanse(LocalDate skjæringstidspunktOpptjening) {
        return lagre(scenario).medSkjæringstidspunkt(Skjæringstidspunkt.builder()
            .medSkjæringstidspunktOpptjening(skjæringstidspunktOpptjening)
            .medSkjæringstidspunktBeregning(skjæringstidspunktOpptjening)
            .build());
    }

    @Test
    public void skal_vente_på_meldekort_når_har_AAP_og_meldekort_uten_AAP_status_etter_første_utta() {
        // Arrange
        LocalDate dagensdato = LocalDate.of(2019, 2, 2);
        LocalDate skjæringstidspunktOpptjening = LocalDate.of(2019, 2, 1);
        LocalDate fom = LocalDate.of(2018, 9, 1);
        LocalDate tom = LocalDate.of(2019, 5, 1);
        LocalDate meldekortFom = LocalDate.of(2018, 12, 1);
        LocalDate meldekortFom2 = LocalDate.of(2019, 1, 12);
        ref = lagReferanse(skjæringstidspunktOpptjening);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagTestUtil.lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, AktivitetStatus.ARBEIDSTAKER);
        iayTestUtil.leggTilAktørytelse(ref, fom, tom, RelatertYtelseTilstand.LØPENDE, "123", FagsakYtelseType.ARBEIDSAVKLARINGSPENGER,
            Collections.emptyList(), Arbeidskategori.ARBEIDSTAKER, lagMeldekortPeriode(meldekortFom), lagMeldekortPeriode(meldekortFom2));

        // Act
        Optional<LocalDate> resultat = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(getAktørYtelse(), bg, dagensdato);

        //Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(skjæringstidspunktOpptjening.plusDays(2));
    }

    @Test
    public void skal_vente_på_meldekort_også_når_har_AAP_status() {
        // Arrange
        LocalDate dagensdato = LocalDate.of(2019, 2, 9);
        LocalDate skjæringstidspunktOpptjening = LocalDate.of(2019, 2, 1);
        LocalDate fom = LocalDate.of(2018, 9, 1);
        LocalDate tom = LocalDate.of(2019, 5, 1);
        LocalDate meldekortFom = LocalDate.of(2018, 12, 1);
        LocalDate meldekortFom2 = LocalDate.of(2019, 1, 12);
        ref = lagReferanse(skjæringstidspunktOpptjening);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagTestUtil.lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
        iayTestUtil.leggTilAktørytelse(ref, fom, tom, RelatertYtelseTilstand.LØPENDE, "123", FagsakYtelseType.ARBEIDSAVKLARINGSPENGER,
            Collections.emptyList(), Arbeidskategori.ARBEIDSTAKER, lagMeldekortPeriode(meldekortFom), lagMeldekortPeriode(meldekortFom2));

        // Act
        Optional<LocalDate> resultat = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(getAktørYtelse(), bg, dagensdato);

        //Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(dagensdato.plusDays(1));
    }

    @Test
    public void skal_ikke_vente_på_meldekort_når_14_dager_etter_første_uttaksdag() {
        // Arrange
        LocalDate dagensdato = LocalDate.of(2019, 2, 16);
        LocalDate skjæringstidspunktOpptjening = LocalDate.of(2019, 2, 1);
        LocalDate fom = LocalDate.of(2018, 9, 1);
        LocalDate tom = LocalDate.of(2019, 5, 1);
        LocalDate meldekortFom = LocalDate.of(2018, 12, 1);
        LocalDate meldekortFom2 = LocalDate.of(2019, 1, 12);
        ref = lagReferanse(skjæringstidspunktOpptjening);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagTestUtil.lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, AktivitetStatus.ARBEIDSTAKER);
        iayTestUtil.leggTilAktørytelse(ref, fom, tom, RelatertYtelseTilstand.LØPENDE, "123", FagsakYtelseType.ARBEIDSAVKLARINGSPENGER,
            Collections.emptyList(), Arbeidskategori.ARBEIDSTAKER, lagMeldekortPeriode(meldekortFom), lagMeldekortPeriode(meldekortFom2));

        // Act
        Optional<LocalDate> resultat = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(getAktørYtelse(), bg, dagensdato);

        //Assert
        assertThat(resultat).isNotPresent();
    }

    @Test
    public void skal_ikke_vente_på_meldekort_når_ikke_har_meldekort_siste_4_måneder() {
        // Arrange
        LocalDate dagensdato = LocalDate.of(2019, 1, 4);
        LocalDate skjæringstidspunktOpptjening = LocalDate.of(2019, 2, 1);
        ref = lagReferanse(skjæringstidspunktOpptjening);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagTestUtil.lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, AktivitetStatus.ARBEIDSTAKER);
        LocalDate fom = LocalDate.of(2018, 9, 1);
        LocalDate tom = LocalDate.of(2019, 5, 1);
        LocalDate meldekortFom = LocalDate.of(2018, 9, 17);
        iayTestUtil.leggTilAktørytelse(ref, fom, tom, RelatertYtelseTilstand.LØPENDE, "123",
            FagsakYtelseType.ARBEIDSAVKLARINGSPENGER, Collections.emptyList(), Arbeidskategori.ARBEIDSTAKER, lagMeldekortPeriode(meldekortFom));

        // Act
        Optional<LocalDate> resultat = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(getAktørYtelse(), bg, dagensdato);

        //Assert
        assertThat(resultat).isNotPresent();
    }

    @Test
    public void skal_ikke_vente_på_meldekort_når_ikke_har_løpende_vedtak() {
        // Arrange
        LocalDate dagensdato = LocalDate.of(2019, 1, 4);
        LocalDate skjæringstidspunktOpptjening = LocalDate.of(2019, 2, 1);
        ref = lagReferanse(skjæringstidspunktOpptjening);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagTestUtil.lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, AktivitetStatus.ARBEIDSTAKER);
        LocalDate fom = LocalDate.of(2018, 9, 1);
        LocalDate tom = LocalDate.of(2018, 12, 31);
        LocalDate meldekortFom = LocalDate.of(2018, 12, 1);
        iayTestUtil.leggTilAktørytelse(ref, fom, tom, RelatertYtelseTilstand.AVSLUTTET, "123",
            FagsakYtelseType.ARBEIDSAVKLARINGSPENGER, Collections.emptyList(), Arbeidskategori.ARBEIDSTAKER, lagMeldekortPeriode(meldekortFom));

        // Act
        Optional<LocalDate> resultat = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(getAktørYtelse(), bg, dagensdato);

        //Assert
        assertThat(resultat).isNotPresent();
    }

    @Test
    public void skal_ikke_vente_på_meldekort_når_er_vanlig_arbeidstaker() {
        // Arrange
        LocalDate dagensdato = LocalDate.of(2019, 1, 4);
        LocalDate skjæringstidspunktOpptjening = LocalDate.of(2019, 2, 1);
        ref = lagReferanse(skjæringstidspunktOpptjening);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagTestUtil.lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, AktivitetStatus.ARBEIDSTAKER);

        // Act
        Optional<LocalDate> resultat = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(getAktørYtelse(), bg, dagensdato);

        //Assert
        assertThat(resultat).isNotPresent();
    }

    @Test
    public void skal_vente_på_meldekort_når_har_DP_og_meldekort_uten_DP_status() {
        // Arrange
        LocalDate dagensdato = LocalDate.of(2019, 1, 4);
        LocalDate skjæringstidspunktOpptjening = LocalDate.of(2019, 2, 1);
        ref = lagReferanse(skjæringstidspunktOpptjening);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagTestUtil.lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, AktivitetStatus.ARBEIDSTAKER);
        LocalDate fom = LocalDate.of(2018, 9, 1);
        LocalDate tom = LocalDate.of(2019, 5, 1);
        LocalDate meldekortFom = LocalDate.of(2018, 5, 1);
        iayTestUtil.leggTilAktørytelse(ref, fom, tom, RelatertYtelseTilstand.LØPENDE, "123", FagsakYtelseType.DAGPENGER,
            Collections.emptyList(), Arbeidskategori.ARBEIDSTAKER, lagMeldekortPeriode(meldekortFom));

        // Act
        Optional<LocalDate> resultat = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(getAktørYtelse(), bg, dagensdato);

        //Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(skjæringstidspunktOpptjening.plusDays(1));
    }

    @Test
    public void skal_vente_på_meldekort_når_har_DP_status() {
        // Arrange
        LocalDate dagensdato = LocalDate.of(2019, 2, 9);
        LocalDate skjæringstidspunktOpptjening = LocalDate.of(2019, 2, 1);
        LocalDate fom = LocalDate.of(2018, 9, 1);
        LocalDate tom = LocalDate.of(2019, 5, 1);
        LocalDate meldekortFom = LocalDate.of(2018, 12, 1);
        LocalDate meldekortFom2 = LocalDate.of(2019, 1, 12);
        ref = lagReferanse(skjæringstidspunktOpptjening);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagTestUtil.lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, AktivitetStatus.DAGPENGER);
        iayTestUtil.leggTilAktørytelse(ref, fom, tom, RelatertYtelseTilstand.LØPENDE, "123", FagsakYtelseType.DAGPENGER,
            Collections.emptyList(), Arbeidskategori.ARBEIDSTAKER, lagMeldekortPeriode(meldekortFom), lagMeldekortPeriode(meldekortFom2));

        // Act
        Optional<LocalDate> resultat = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(getAktørYtelse(), bg, dagensdato);

        //Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(dagensdato.plusDays(1));
    }

    @Test
    public void skal_vente_på_meldekort_når_ikke_har_løpende_vedtak_men_var_løpende_til_skjæringstidspunkt() {
        // Arrange
        LocalDate dagensdato = LocalDate.of(2019, 2, 12);
        LocalDate skjæringstidspunktOpptjening = LocalDate.of(2019, 2, 11);
        ref = lagReferanse(skjæringstidspunktOpptjening);
        BeregningsgrunnlagEntitet bg = beregningsgrunnlagTestUtil.lagGjeldendeBeregningsgrunnlag(ref, skjæringstidspunktOpptjening, AktivitetStatus.ARBEIDSTAKER);
        LocalDate fom = LocalDate.of(2018, 9, 3);
        LocalDate tom = LocalDate.of(2019, 2, 11);
        LocalDate meldekortFom = LocalDate.of(2019, 1, 21);
        iayTestUtil.leggTilAktørytelse(ref, fom, tom, RelatertYtelseTilstand.AVSLUTTET, "123", FagsakYtelseType.ARBEIDSAVKLARINGSPENGER,
            Collections.emptyList(), Arbeidskategori.ARBEIDSTAKER, lagMeldekortPeriode(meldekortFom));

        // Act
        Optional<LocalDate> resultat = AutopunktUtlederFastsettBeregningsaktiviteterTjeneste.skalVenteTilDatoPåMeldekortAAPellerDP(getAktørYtelse(), bg, dagensdato);

        //Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(dagensdato.plusDays(1));
    }

    private Periode lagMeldekortPeriode(LocalDate fom) {
        return Periode.of(fom, fom.plusDays(13));
    }

}
