package no.nav.folketrygdloven.beregningsgrunnlag.output;

import java.util.List;

import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAvslagsårsak;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;

public class KalkulusResultat {

    private List<BeregningAksjonspunktResultat> beregningAksjonspunktResultat;
    private Boolean vilkårOppfylt;
    private Avslagsårsak avslagsårsak;

    public KalkulusResultat(List<BeregningAksjonspunktResultat> beregningAksjonspunktResultat) {
        this.beregningAksjonspunktResultat = beregningAksjonspunktResultat;
    }

    public KalkulusResultat medVilkårResulatat(boolean vilkårOppfylt) {
        this.vilkårOppfylt = vilkårOppfylt;
        return this;
    }

    public KalkulusResultat medAvslåttVilkår(Avslagsårsak avslagsårsak) {
        this.vilkårOppfylt = false;
        this.avslagsårsak = avslagsårsak;
        return this;
    }

    public List<BeregningAksjonspunktResultat> getBeregningAksjonspunktResultat() {
        return beregningAksjonspunktResultat;
    }

    public Boolean getVilkårOppfylt() {
        return vilkårOppfylt;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }
}
