package no.nav.folketrygdloven.beregningsgrunnlag.output;

import java.util.List;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;

public class BeregningsgrunnlagRegelResultat {
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private List<BeregningAksjonspunktResultat> aksjonspunkter;
    private Boolean vilkårOppfylt;

    public BeregningsgrunnlagRegelResultat(BeregningsgrunnlagEntitet beregningsgrunnlag, List<BeregningAksjonspunktResultat> aksjonspunktResultatListe) {
        this.beregningsgrunnlag = beregningsgrunnlag;
        this.aksjonspunkter = aksjonspunktResultatListe;
    }

    public BeregningsgrunnlagEntitet getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    public List<BeregningAksjonspunktResultat> getAksjonspunkter() {
        return aksjonspunkter;
    }

    public Boolean getVilkårOppfylt() {
        return vilkårOppfylt;
    }

    public void setVilkårOppfylt(Boolean vilkårOppfylt) {
        this.vilkårOppfylt = vilkårOppfylt;
    }
}
