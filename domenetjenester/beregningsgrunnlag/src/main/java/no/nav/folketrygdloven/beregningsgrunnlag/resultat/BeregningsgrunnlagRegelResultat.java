package no.nav.folketrygdloven.beregningsgrunnlag.resultat;

import java.util.List;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;

public class BeregningsgrunnlagRegelResultat {
    private Beregningsgrunnlag beregningsgrunnlag;
    private List<BeregningAksjonspunktResultat> aksjonspunkter;
    private Boolean vilkårOppfylt;

    public BeregningsgrunnlagRegelResultat(Beregningsgrunnlag beregningsgrunnlag, List<BeregningAksjonspunktResultat> aksjonspunktResultatListe) {
        this.beregningsgrunnlag = beregningsgrunnlag;
        this.aksjonspunkter = aksjonspunktResultatListe;
    }

    public Beregningsgrunnlag getBeregningsgrunnlag() {
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
