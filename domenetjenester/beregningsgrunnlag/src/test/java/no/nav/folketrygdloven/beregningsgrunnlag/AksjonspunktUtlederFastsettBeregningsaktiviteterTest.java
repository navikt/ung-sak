package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktDefinisjon;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningVenteårsak;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.AbstractTestScenario;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.ytelse.fp.AksjonspunktUtlederFastsettBeregningsaktiviteterFP;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvistBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;

public class AksjonspunktUtlederFastsettBeregningsaktiviteterTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private RepositoryProvider repositoryProvider = Mockito.spy(new RepositoryProvider(entityManager));

    private AvklarAktiviteterTjeneste avklarAktiviteterTjeneste = mock(AvklarAktiviteterTjeneste.class);
    private AksjonspunktUtlederFastsettBeregningsaktiviteter aksjonspunktUtleder;
    private BeregningAktivitetAggregatEntitet beregningAktivitetAggregat;
    private BehandlingReferanse ref;

    private BeregningsperiodeTjeneste beregningsperiodeTjeneste = mock(BeregningsperiodeTjeneste.class);
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private boolean erOverstyrt;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private OpptjeningAktiviteter opptjeningAktiviteter = new OpptjeningAktiviteter();

    private InntektArbeidYtelseGrunnlag iayMock = mock(InntektArbeidYtelseGrunnlag.class);
    private AktørYtelse ay = mock(AktørYtelse.class);

    @Before
    public void setUp() {
        erOverstyrt = false;
        LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        beregningAktivitetAggregat = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
            .build();
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        scenario.medDefaultInntektArbeidYtelse();
        ref = lagre(scenario, SKJÆRINGSTIDSPUNKT);
        when(beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(any(), anyList(), any())).thenReturn(false);
        aksjonspunktUtleder = new AksjonspunktUtlederFastsettBeregningsaktiviteterFP(avklarAktiviteterTjeneste, beregningsperiodeTjeneste);
    }

    private BehandlingReferanse lagre(AbstractTestScenario<?> scenario, LocalDate skjæringstidspunkt) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening).medSkjæringstidspunkt(Skjæringstidspunkt.builder()
            .medFørsteUttaksdato(skjæringstidspunkt)
            .medUtledetSkjæringstidspunkt(skjæringstidspunkt)
            .medSkjæringstidspunktOpptjening(skjæringstidspunkt)
            .build());
    }

    @Test
    public void skal_returnere_aksjonspunkt_for_avklare_aktiviteter() {
        when(avklarAktiviteterTjeneste.skalAvklareAktiviteter(any(), any(), any())).thenReturn(true);
        List<BeregningAksjonspunktResultat> resultat = aksjonspunktUtleder.utledAksjonspunkterFor(ref, beregningsgrunnlag, beregningAktivitetAggregat, erOverstyrt, lagBeregningsgrunnlagInput());

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getBeregningAksjonspunktDefinisjon()).isEqualTo(BeregningAksjonspunktDefinisjon.AVKLAR_AKTIVITETER);
    }


    @Test
    public void skal_ikkje_returnere_aksjonspunkt() {
        when(avklarAktiviteterTjeneste.skalAvklareAktiviteter(any(), any(), any())).thenReturn(false);
        List<BeregningAksjonspunktResultat> resultat = aksjonspunktUtleder.utledAksjonspunkterFor(ref, beregningsgrunnlag, beregningAktivitetAggregat, erOverstyrt, lagBeregningsgrunnlagInput());
        assertThat(resultat).isEmpty();
    }

    private InntektArbeidYtelseGrunnlag getIAYGrunnlag() {
        return iayTjeneste.hentGrunnlag(ref.getBehandlingId());
    }

    private InntektArbeidYtelseGrunnlag getMockedIAYGrunnlag() {
        return iayMock;
    }


    @Test
    public void skalSettePåVentNårFørRapporteringsfrist() {
        // Arrange
        when(beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(any(), anyList(), any())).thenReturn(true);
        LocalDate frist = LocalDate.of(2018, 10, 5);
        when(beregningsperiodeTjeneste.utledBehandlingPåVentFrist(any())).thenReturn(frist);

        //Act
        List<BeregningAksjonspunktResultat> resultater = aksjonspunktUtleder.utledAksjonspunkterFor(ref, beregningsgrunnlag, beregningAktivitetAggregat, erOverstyrt, lagBeregningsgrunnlagInput());

        // Assert
        assertThat(resultater).hasSize(1);
        BeregningAksjonspunktResultat beregningAksjonspunktResultat = resultater.get(0);
        assertThat(beregningAksjonspunktResultat.getBeregningAksjonspunktDefinisjon()).isEqualTo(BeregningAksjonspunktDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST);
        assertThat(beregningAksjonspunktResultat.getVentefrist()).isNotNull();
        assertThat(beregningAksjonspunktResultat.getVenteårsak()).isNotNull();

        assertThat(beregningAksjonspunktResultat.getVenteårsak()).isEqualTo(BeregningVenteårsak.VENT_INNTEKT_RAPPORTERINGSFRIST);
        assertThat(beregningAksjonspunktResultat.getVentefrist()).isEqualTo(frist.atStartOfDay());
    }

    @Test
    public void skalUtledeAutopunktVentPåMeldekort() {
        // Arrange
        YtelseBuilder yb = YtelseBuilder.oppdatere(Optional.empty());
        Ytelse ytelse = yb.medYtelseType(FagsakYtelseType.DAGPENGER)
            .medPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(2)))
            .medKilde(Fagsystem.ARENA)
            .medStatus(RelatertYtelseTilstand.LØPENDE)
            .medYtelseAnvist(lagYtelseAnvist(yb.getAnvistBuilder()))
            .build();

        List<Ytelse> liste = Collections.singletonList(ytelse);
        when(iayMock.getAktørYtelseFraRegister(any())).thenReturn(Optional.of(ay));
        when(ay.getAlleYtelser()).thenReturn(liste);

        // Act
        List<BeregningAksjonspunktResultat> resultater = aksjonspunktUtleder.utledAksjonspunkterFor(ref, beregningsgrunnlag, beregningAktivitetAggregat, erOverstyrt, lagMockBeregningsgrunnlagInput());

        // Assert
        assertThat(resultater).hasSize(1);
        assertThat(resultater).anySatisfy(resultat ->
            assertThat(resultat.getBeregningAksjonspunktDefinisjon()).isEqualTo(BeregningAksjonspunktDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT)
        );
    }

    private YtelseAnvist lagYtelseAnvist(YtelseAnvistBuilder anvistBuilder) {
        return anvistBuilder.medAnvistPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(1))).build();
    }

    @Test
    public void skalUtledeAutopunktVentPåInntektFrilansNårManSkalVentePåBådeInntekterFrilansOgAAPMeldekort() {
        // Arrange
        when(beregningsperiodeTjeneste.skalVentePåInnrapporteringAvInntekt(any(), anyList(), any())).thenReturn(true);
        LocalDate frist = LocalDate.of(2018, 10, 5);
        when(beregningsperiodeTjeneste.utledBehandlingPåVentFrist(any())).thenReturn(frist);

        // Act
        List<BeregningAksjonspunktResultat> resultater = aksjonspunktUtleder.utledAksjonspunkterFor(ref, beregningsgrunnlag, beregningAktivitetAggregat, erOverstyrt, lagBeregningsgrunnlagInput());

        // Assert
        assertThat(resultater).hasSize(1);
        assertThat(resultater).anySatisfy(resultat ->
            assertThat(resultat.getBeregningAksjonspunktDefinisjon()).isEqualTo(BeregningAksjonspunktDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST)
        );
    }

    private BeregningsgrunnlagInput lagBeregningsgrunnlagInput() {
        return new BeregningsgrunnlagInput(ref, getIAYGrunnlag(), opptjeningAktiviteter, null, new K9BeregningsgrunnlagInput());
    }

    private BeregningsgrunnlagInput lagMockBeregningsgrunnlagInput() {
        return new BeregningsgrunnlagInput(ref, getMockedIAYGrunnlag(), opptjeningAktiviteter, null, new K9BeregningsgrunnlagInput());
    }

}
