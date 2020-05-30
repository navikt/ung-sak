package no.nav.folketrygdloven.beregningsgrunnlag.output;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class KalkulusResultat {

    private List<BeregningAksjonspunktResultat> beregningAksjonspunktResultat;
    private Boolean vilkårOppfylt;
    private Avslagsårsak avslagsårsak;
    private Map<DatoIntervallEntitet, BeregningVilkårResultat> vilkårResultatPrPeriode = new HashMap<>();

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

    public KalkulusResultat leggTilVilkårResultat(DatoIntervallEntitet periode, boolean vilkårOppfylt, Avslagsårsak avslagsårsak) {
        this.vilkårResultatPrPeriode.put(periode, new BeregningVilkårResultat(vilkårOppfylt, avslagsårsak));
        return this;
    }

    public List<BeregningAksjonspunktResultat> getBeregningAksjonspunktResultat() {
        return beregningAksjonspunktResultat;
    }

    public Map<DatoIntervallEntitet, BeregningVilkårResultat> getVilkårResultatPrPeriode() {
        return vilkårResultatPrPeriode;
    }

    public Boolean getVilkårOppfylt() {
        return vilkårOppfylt;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<vilkårOppfylt=" + vilkårOppfylt +
            ", avslagsårsak=" + avslagsårsak +
            ", beregningAksjonspunktResultat=" + beregningAksjonspunktResultat +
            ">";
    }

}
