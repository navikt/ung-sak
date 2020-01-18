package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.List;

import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;

/**
 * Builder for å modifisere et vilkårResultat.
 */
public class VilkårResultatBuilder {

    private VilkårResultat kladd = new VilkårResultat();
    private boolean built;

    VilkårResultatBuilder() {
        super();
    }

    VilkårResultatBuilder(VilkårResultat eksisterendeResultat) {
        super();
        this.kladd = eksisterendeResultat;
    }

    public static VilkårResultatBuilder kopi(VilkårResultat origVilkårResultat) {
        return new VilkårResultatBuilder(new VilkårResultat(origVilkårResultat));
    }

    public VilkårResultatBuilder leggTilIkkeVurderteVilkår(List<DatoIntervallEntitet> intervaller, VilkårType... vilkår) {
        leggTilIkkeVurderteVilkår(intervaller, List.of(vilkår));
        return this;
    }

    public VilkårBuilder hentBuilderFor(VilkårType vilkårType) {
        final var vilkåret = kladd.getVilkårene().stream().filter(v -> vilkårType.equals(v.getVilkårType())).findFirst().orElse(new Vilkår());
        return new VilkårBuilder(vilkåret)
            .medType(vilkårType);
    }

    public VilkårResultatBuilder leggTil(VilkårBuilder vilkårBuilder) {
        kladd.leggTilVilkår(vilkårBuilder.build());
        return this;
    }

    /**
     * OBS: Returnerer alltid nytt vilkårresultat.
     */
    public VilkårResultat build() {
        if (built) throw new IllegalStateException("Kan ikke bygge to ganger med samme builder");
        built = true;
        return kladd;
    }

    public VilkårResultatBuilder fjernVilkår(VilkårType vilkårType) {
        kladd.fjernVilkår(vilkårType);
        return this;
    }

    public VilkårResultatBuilder leggTilIkkeVurderteVilkår(List<DatoIntervallEntitet> intervaller, List<VilkårType> vilkår) {
        vilkår.stream()
            .map(type -> new VilkårBuilder().medType(type))
            .peek(v -> intervaller.forEach(p -> v.leggTil(v.hentBuilderFor(p.getFomDato(), p.getTomDato()).medUtfall(Utfall.IKKE_VURDERT))))
            .forEach(builder -> kladd.leggTilVilkår(builder.build()));
        return this;
    }
}
