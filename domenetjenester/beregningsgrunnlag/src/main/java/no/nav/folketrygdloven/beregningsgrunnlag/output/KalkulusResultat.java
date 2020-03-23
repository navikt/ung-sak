package no.nav.folketrygdloven.beregningsgrunnlag.output;

import java.util.List;

public class KalkulusResultat {

    private List<BeregningAksjonspunktResultat> beregningAksjonspunktResultat;
    private Boolean vilkårOppfylt;

    public KalkulusResultat(List<BeregningAksjonspunktResultat> beregningAksjonspunktResultat) {
        this.beregningAksjonspunktResultat = beregningAksjonspunktResultat;
    }

    public KalkulusResultat medVilkårResulatat(boolean vilkårOppfylt) {
        this.vilkårOppfylt = vilkårOppfylt;
        return this;
    }

    public List<BeregningAksjonspunktResultat> getBeregningAksjonspunktResultat() {
        return beregningAksjonspunktResultat;
    }

    public Boolean getVilkårOppfylt() {
        return vilkårOppfylt;
    }
}
