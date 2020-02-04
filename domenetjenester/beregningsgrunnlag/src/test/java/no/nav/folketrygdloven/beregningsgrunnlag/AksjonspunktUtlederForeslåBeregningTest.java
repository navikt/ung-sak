package no.nav.folketrygdloven.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.K9BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelMerknad;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.ResultatBeregningType;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAksjonspunktDefinisjon;

public class AksjonspunktUtlederForeslåBeregningTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());

    private AksjonspunktUtlederForeslåBeregning utleder;
    private BehandlingReferanse referanse;


    @Before
    public void setup() {
        var scenario = TestScenarioBuilder.nyttScenario();
        referanse = scenario.lagre(repositoryProvider);
        utleder = new AksjonspunktUtlederForeslåBeregning();
    }

    @Test
    public void skalIkkeFåAksjonspunkterVed100PDekningsgrad() {
        // Act
        List<BeregningAksjonspunktResultat> aksjonspunkter = utleder.utledAksjonspunkter(lagInput(referanse), Collections.emptyList());
        // Assert
        assertThat(aksjonspunkter).isEmpty();
    }

    @Test
    public void skalIkkeFåAksjonspunkterVed80PDekningsgrad() {
        // Arrange
        var input = lagInput();
        // Act
        List<BeregningAksjonspunktResultat> aksjonspunkter = utleder.utledAksjonspunkter(input, Collections.emptyList());
        // Assert
        assertThat(aksjonspunkter).isEmpty();
    }

    @Test
    public void skalFåAksjonspunkt5042() {
        // Arrange
        RegelResultat regelResultat = lagRegelResultat("5042");
        // Act
        List<BeregningAksjonspunktResultat> aksjonspunkter = utleder.utledAksjonspunkter(lagInput(referanse), Collections.singletonList(regelResultat));
        // Assert
        var apDefs = aksjonspunkter.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).containsExactly(BeregningAksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    @Test
    public void skalFåAksjonspunkt5049() {
        // Arrange
        RegelResultat regelResultat = lagRegelResultat("5049");
        // Act
        List<BeregningAksjonspunktResultat> aksjonspunkter = utleder.utledAksjonspunkter(lagInput(referanse), Collections.singletonList(regelResultat));
        // Assert
        var apDefs = aksjonspunkter.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).containsExactly(BeregningAksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET);
    }

    @Test
    public void skalFåAksjonspunkt5038() {
        // Arrange
        RegelResultat regelResultat = lagRegelResultat("5038");

        var input = lagInput();

        // Act
        List<BeregningAksjonspunktResultat> aksjonspunkter = utleder.utledAksjonspunkter(input, Collections.singletonList(regelResultat));
        // Assert
        var apDefs = aksjonspunkter.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).containsExactlyInAnyOrder(
            BeregningAksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS
        );
    }

    private BeregningsgrunnlagInput lagInput() {
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        return new BeregningsgrunnlagInput(referanse, null, null, null, foreldrepengerGrunnlag);
    }

    @Test
    public void skal_ikke_få_aksjonspunkt5087_når_barnet_ikke_har_dødd() {
        // Arrange
        // Act
        List<BeregningAksjonspunktResultat> aksjonspunkter = utleder.utledAksjonspunkter(lagInput(referanse), Collections.emptyList());
        // Assert
        var apDefs = aksjonspunkter.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt5087_når_ingen_barn_et_født() {
        // Arrange
        var input = lagInput();
        // Act
        List<BeregningAksjonspunktResultat> aksjonspunkter = utleder.utledAksjonspunkter(input, Collections.emptyList());
        // Assert
        var apDefs = aksjonspunkter.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt5087_når_ikke_alle_barnene_døde_innen_seks_uker() {
        // Arrange
        var input = lagInput();

        // Act
        List<BeregningAksjonspunktResultat> aksjonspunkter = utleder.utledAksjonspunkter(input, Collections.emptyList());
        // Assert
        var apDefs = aksjonspunkter.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt5087_når_ett_barn_døde_og_ett_barn_levde() {
        // Arrange
        var input = lagInput();

        // Act
        List<BeregningAksjonspunktResultat> aksjonspunkter = utleder.utledAksjonspunkter(input, Collections.emptyList());
        // Assert
        var apDefs = aksjonspunkter.stream().map(BeregningAksjonspunktResultat::getBeregningAksjonspunktDefinisjon).collect(Collectors.toList());
        assertThat(apDefs).isEmpty();
    }

    private BeregningsgrunnlagInput lagInput(BehandlingReferanse referanse) {
        var foreldrepengerGrunnlag = new K9BeregningsgrunnlagInput();
        return new BeregningsgrunnlagInput(referanse, null, null, null, foreldrepengerGrunnlag);
    }

    private RegelResultat lagRegelResultat(String merknadKode) {
        RegelMerknad regelMerknad = new RegelMerknad(merknadKode, "blablabla");
        return new RegelResultat(ResultatBeregningType.IKKE_BEREGNET, "regelInput", "regelSporing")
            .medRegelMerknad(regelMerknad);
    }

}
