package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import java.util.Objects;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.typer.Periode;

public class VilkårPeriodeResultatDto {

    private Periode periode;

    private Avslagsårsak avslagsårsak;

    private Utfall utfall = Utfall.IKKE_VURDERT;

    private VilkårType vilkårType;

    public VilkårPeriodeResultatDto(VilkårType vilkårType, Periode periode, Avslagsårsak avslagsårsak, Utfall utfall) {
        this.vilkårType = Objects.requireNonNull(vilkårType, "vilkårType");
        this.periode = Objects.requireNonNull(periode, "periode");
        this.utfall = utfall == null ? Utfall.IKKE_VURDERT : utfall;
        this.avslagsårsak = this.utfall.equals(Utfall.IKKE_OPPFYLT) ? Objects.requireNonNull(avslagsårsak, "avslagsårsak") : avslagsårsak;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }

    public Utfall getUtfall() {
        return utfall;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vilkårType, periode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !obj.getClass().equals(this.getClass()))
            return false;
        var other = (VilkårPeriodeResultatDto) obj;
        return Objects.equals(vilkårType, other.vilkårType)
            && Objects.equals(periode, other.periode);
    }
}
