package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.List;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;

public interface BeregningsgrunnlagAksjonspunktUtleder {

    List<BeregningAksjonspunktResultat> utledAksjonspunkterFor(BeregningsgrunnlagInput input,
                                                               BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag,
                                                               boolean erOverstyrt);

}
