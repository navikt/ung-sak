package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import java.util.List;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Builder for å modifisere et vilkårResultat.
 */
public class VilkårResultatBuilder {

    private Vilkårene kladd = new Vilkårene();
    private int mellomliggendePeriodeAvstand = 0;
    private boolean built;

    public VilkårResultatBuilder() {
        super();
    }

    VilkårResultatBuilder(Vilkårene eksisterendeResultat) {
        super();
        if (eksisterendeResultat != null) {
            this.kladd = new Vilkårene(eksisterendeResultat);
        }
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

    public VilkårResultatBuilder medMaksMellomliggendePeriodeAvstand(int mellomliggendePeriodeAvstand) {
        if (mellomliggendePeriodeAvstand < 0) {
            throw new IllegalArgumentException("Må være positivt");
        }
        this.mellomliggendePeriodeAvstand = mellomliggendePeriodeAvstand;
        return this;
    }


    public VilkårResultatBuilder leggTil(VilkårBuilder vilkårBuilder) {
        kladd.leggTilVilkår(vilkårBuilder.build());
        return this;
    }

    /**
     * OBS: Returnerer alltid nytt vilkårresultat.
     */
    public Vilkårene build() {
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
            .map(type -> hentBuilderFor(type)
                .medType(type)
                .medMaksMellomliggendePeriodeAvstand(mellomliggendePeriodeAvstand))
            .peek(v -> intervaller.forEach(p -> v.leggTil(v.hentBuilderFor(p.getFomDato(), p.getTomDato()).medUtfall(Utfall.IKKE_VURDERT))))
            .forEach(builder -> kladd.leggTilVilkår(builder.build()));
        return this;
    }
}
