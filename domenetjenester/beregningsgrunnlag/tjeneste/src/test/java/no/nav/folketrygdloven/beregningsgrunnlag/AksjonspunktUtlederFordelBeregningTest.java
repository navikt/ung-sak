package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktDefinisjon;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;

public class AksjonspunktUtlederFordelBeregningTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.MARCH, 23);

    @Mock
    private FordelBeregningsgrunnlagTjeneste fordelBeregningsgrunnlagTjenesteMock;

    private AksjonspunktUtlederFordelBeregning aksjonspunktUtlederFordelBeregning;

    private BeregningAktivitetAggregatEntitet.Builder beregningAktivitetBuilder = BeregningAktivitetAggregatEntitet.builder()
        .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING);

    private BehandlingReferanse behandlingReferanse;

    private static final Long BEHANDLING_ID = 123L;

    @Before
    public void setup() {
        initMocks(this);
        TestScenarioBuilder scenario = TestScenarioBuilder.nyttScenario();
        behandlingReferanse = scenario.lagMocked().medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING);
        aksjonspunktUtlederFordelBeregning = new AksjonspunktUtlederFordelBeregning(fordelBeregningsgrunnlagTjenesteMock);
    }

    @Test
    public void skal_ikke_lage_aksjonspunkt_dersom_det_ikke_er_endret_bg() {
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagGrunnlag();

        settOppMocks(Optional.empty());

        List<BeregningAksjonspunktResultat> aksjonspunktResultats = utledAksjonspunkter(behandlingReferanse, grunnlag);

        assertThat(aksjonspunktResultats).isEmpty();
    }

    private List<BeregningAksjonspunktResultat> utledAksjonspunkter(BehandlingReferanse ref, BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        List<BeregningAksjonspunktResultat> aksjonspunktResultats = aksjonspunktUtlederFordelBeregning.utledAksjonspunkterFor(ref, grunnlag, null, List.of());
        return aksjonspunktResultats;
    }

    @Test
    public void skal_lage_aksjonspunkt_når_det_er_endring() {
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagGrunnlag();

        settOppMocks(Optional.of(FordelBeregningsgrunnlagTjeneste.VurderManuellBehandling.NYTT_ARBEIDSFORHOLD));

        List<BeregningAksjonspunktResultat> aksjonspunktResultats = utledAksjonspunkter(behandlingReferanse, grunnlag);

        assertThat(aksjonspunktResultats).hasSize(1);
        assertThat(aksjonspunktResultats.get(0).getBeregningAksjonspunktDefinisjon()).isEqualTo(BeregningAksjonspunktDefinisjon.FORDEL_BEREGNINGSGRUNNLAG);
    }

    private void settOppMocks(Optional<FordelBeregningsgrunnlagTjeneste.VurderManuellBehandling> vurderEndring) {
        when(fordelBeregningsgrunnlagTjenesteMock
            .vurderManuellBehandling(any(BeregningsgrunnlagEntitet.class), any(BeregningAktivitetAggregatEntitet.class), any(), any()))
            .thenReturn(vurderEndring);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagGrunnlag(FaktaOmBeregningTilfelle... tilfeller) {
        List<FaktaOmBeregningTilfelle> listeMedTilfeller = Arrays.asList(tilfeller);

        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medGrunnbeløp(BigDecimal.valueOf(GrunnbeløpTestKonstanter.GRUNNBELØP_2018))
            .leggTilFaktaOmBeregningTilfeller(listeMedTilfeller)
            .build();

        BeregningsgrunnlagGrunnlagEntitet grunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medRegisterAktiviteter(beregningAktivitetBuilder.build())
            .build(BEHANDLING_ID, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        return grunnlag;

    }

}
