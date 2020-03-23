package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import java.util.Objects;

import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;

class WrappedVilkårPeriode {

    private final VilkårPeriode vilkårPeriode;

    public WrappedVilkårPeriode(VilkårPeriode vilkårPeriode) {
        Objects.requireNonNull(vilkårPeriode, "vilkårPeriode");
        this.vilkårPeriode = vilkårPeriode;
    }

    public boolean getErOverstyrt() {
        return vilkårPeriode.getErOverstyrt();
    }

    public VilkårPeriode getVilkårPeriode() {
        return vilkårPeriode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedVilkårPeriode that = (WrappedVilkårPeriode) o;
        return Objects.equals(vilkårPeriode.getUtfall(), that.vilkårPeriode.getUtfall()) &&
            Objects.equals(vilkårPeriode.getOverstyrtUtfall(), that.vilkårPeriode.getOverstyrtUtfall()) &&
            Objects.equals(vilkårPeriode.getAvslagsårsak(), that.vilkårPeriode.getAvslagsårsak()) &&
            Objects.equals(vilkårPeriode.getErOverstyrt(), that.vilkårPeriode.getErOverstyrt()) &&
            Objects.equals(vilkårPeriode.getErManueltVurdert(), that.vilkårPeriode.getErManueltVurdert()) &&
            Objects.equals(vilkårPeriode.getBegrunnelse(), that.vilkårPeriode.getBegrunnelse());
    }

    @Override
    public int hashCode() {
        return Objects.hash(vilkårPeriode.getUtfall(),
            vilkårPeriode.getOverstyrtUtfall(),
            vilkårPeriode.getAvslagsårsak(),
            vilkårPeriode.getErOverstyrt(),
            vilkårPeriode.getErManueltVurdert(),
            vilkårPeriode.getBegrunnelse());
    }

}
