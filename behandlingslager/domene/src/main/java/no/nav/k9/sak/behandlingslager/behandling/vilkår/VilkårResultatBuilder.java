package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Builder for å modifisere et vilkårResultat.
 */
public class VilkårResultatBuilder {

    private static final Logger log = LoggerFactory.getLogger(VilkårResultatBuilder.class);

    private Vilkårene kladd = new Vilkårene();
    private int mellomliggendePeriodeAvstand = 0;
    private KantIKantVurderer kantIKantVurderer = new IngenVurdering();
    private boolean built;
    private DatoIntervallEntitet boundry = DatoIntervallEntitet.fraOgMedTilOgMed(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);
    private LocalDateTimeline<WrappedVilkårPeriode> fagsakTidslinje = null;

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

    public VilkårResultatBuilder medKantIKantVurderer(KantIKantVurderer vurderer) {
        Objects.requireNonNull(vurderer);
        this.kantIKantVurderer = vurderer;
        return this;
    }

    public VilkårResultatBuilder leggTil(VilkårBuilder vilkårBuilder) {
        if (fagsakTidslinje != null) {
            vilkårBuilder.medFagsaksTidslinje(fagsakTidslinje);
        }
        kladd.leggTilVilkår(vilkårBuilder.build());
        return this;
    }

    /**
     * OBS: Returnerer alltid nytt vilkårresultat.
     */
    public Vilkårene build() {
        if (built) throw new IllegalStateException("Kan ikke bygge to ganger med samme builder");
        validerPerioder();
        built = true;
        return kladd;
    }

    private void validerPerioder() {
        var invalidVilkårMap = new HashMap<VilkårType, List<VilkårPeriode>>();
        kladd.getVilkårene().forEach(v -> invalidVilkårMap.put(v.getVilkårType(), v.getPerioder().stream().filter(it -> gårUtenforBoundry(it.getPeriode())).collect(Collectors.toList())));
        var harPerioderSomGårUtoverGrensen = invalidVilkårMap.entrySet().stream().anyMatch(it -> !it.getValue().isEmpty());
        if (harPerioderSomGårUtoverGrensen) {
            log.warn("Behandligen har vilkår med perioder[{}] som strekker seg utover maks({}) for fagsaken", invalidVilkårMap, boundry);
            throw new IllegalStateException("Behandligen har perioder som strekker seg utover maks(" + boundry + ") for fagsaken");
        }
    }

    private boolean gårUtenforBoundry(DatoIntervallEntitet periode) {
        if (boundry.getFomDato().isAfter(periode.getFomDato())) {
            return true;
        }
        return boundry.getTomDato().isBefore(periode.getTomDato());
    }

    public VilkårResultatBuilder fjernVilkår(VilkårType vilkårType) {
        kladd.fjernVilkår(vilkårType);
        return this;
    }

    public VilkårResultatBuilder leggTilIkkeVurderteVilkår(List<DatoIntervallEntitet> intervaller, List<VilkårType> vilkår) {
        vilkår.stream()
            .map(type -> hentBuilderFor(type)
                .medType(type)
                .medMaksMellomliggendePeriodeAvstand(mellomliggendePeriodeAvstand)
                .medKantIKantVurderer(kantIKantVurderer))
            .peek(v -> intervaller.forEach(p -> v.leggTil(v.hentBuilderFor(p.getFomDato(), p.getTomDato()).medUtfall(Utfall.IKKE_VURDERT))))
            .forEach(builder -> kladd.leggTilVilkår(builder.build()));
        return this;
    }

    public VilkårResultatBuilder leggTilIkkeVurderteVilkår(Map<VilkårType, NavigableSet<DatoIntervallEntitet>> vilkårPeriodeMap, NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles) {
        vilkårPeriodeMap.forEach((k, v) -> {
            var builder = hentBuilderFor(k)
                .medType(k)
                .medMaksMellomliggendePeriodeAvstand(mellomliggendePeriodeAvstand)
                .medKantIKantVurderer(kantIKantVurderer)
                .tilbakestill(perioderSomSkalTilbakestilles);
            v.forEach(periode -> builder.leggTil(builder.hentBuilderFor(periode.getFomDato(), periode.getTomDato()).medUtfall(Utfall.IKKE_VURDERT)));
            kladd.leggTilVilkår(builder.build());
        });
        return this;
    }

    public VilkårResultatBuilder medBoundry(DatoIntervallEntitet periode) {
        this.boundry = periode;
        return this;
    }

    public VilkårResultatBuilder medFagsakTidslinje(VilkårBuilder vilkårBuilder) {
        this.fagsakTidslinje = vilkårBuilder.getTidslinje();
        return this;
    }
}
