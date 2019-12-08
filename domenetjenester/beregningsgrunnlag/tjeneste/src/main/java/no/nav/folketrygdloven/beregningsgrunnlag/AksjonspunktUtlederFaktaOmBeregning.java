package no.nav.folketrygdloven.beregningsgrunnlag;

import static java.util.Collections.singletonList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FaktaOmBeregningTilfelleTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktDefinisjon;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;

@ApplicationScoped
public class AksjonspunktUtlederFaktaOmBeregning implements BeregningsgrunnlagAksjonspunktUtleder {

    private FaktaOmBeregningTilfelleTjeneste faktaOmBeregningTilfelleTjeneste;

    AksjonspunktUtlederFaktaOmBeregning() {
        // for CDI proxy
    }

    @Inject
    public AksjonspunktUtlederFaktaOmBeregning(FaktaOmBeregningTilfelleTjeneste faktaOmBeregningTilfelleTjeneste) {
        this.faktaOmBeregningTilfelleTjeneste = faktaOmBeregningTilfelleTjeneste;
    }

    @Override
    public List<BeregningAksjonspunktResultat> utledAksjonspunkterFor(BeregningsgrunnlagInput input,
                                                                      BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag,
                                                                      boolean erOverstyrt) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagGrunnlag.getBeregningsgrunnlag().orElse(null);
        Objects.requireNonNull(beregningsgrunnlag, "beregningsgrunnlag");

        List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = faktaOmBeregningTilfelleTjeneste.finnTilfellerForFellesAksjonspunkt(input,
            beregningsgrunnlagGrunnlag);
        BeregningsgrunnlagEntitet.builder(beregningsgrunnlag).leggTilFaktaOmBeregningTilfeller(faktaOmBeregningTilfeller);
        if (erOverstyrt) {
            return singletonList(BeregningAksjonspunktResultat.opprettFor(BeregningAksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG));
        }
        if (faktaOmBeregningTilfeller.isEmpty()) {
            return Collections.emptyList();
        }
        return singletonList(BeregningAksjonspunktResultat.opprettFor(BeregningAksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN));
    }
}
