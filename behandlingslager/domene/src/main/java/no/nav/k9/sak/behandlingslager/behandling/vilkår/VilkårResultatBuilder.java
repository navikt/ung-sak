package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
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
    private LocalDateInterval boundry = new LocalDateInterval(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);
    private LocalDateTimeline<WrappedVilkårPeriode> fullstendigTidslinje = null;

    private boolean kappOverskytende;

    public VilkårResultatBuilder() {
    }

    VilkårResultatBuilder(Vilkårene eksisterendeResultat) {
        if (eksisterendeResultat != null) {
            this.kladd = new Vilkårene(eksisterendeResultat);
        }
    }

    public VilkårResultatBuilder leggTilIkkeVurderteVilkår(List<DatoIntervallEntitet> intervaller, VilkårType... vilkår) {
        leggTilIkkeVurderteVilkår(intervaller, List.of(vilkår));
        return this;
    }

    public VilkårBuilder hentBuilderFor(VilkårType vilkårType) {
        final var vilkåret = kladd.getVilkårene().stream().filter(v -> vilkårType.equals(v.getVilkårType())).findFirst().orElse(new Vilkår(vilkårType));
        var vilkårBuilder = new VilkårBuilder(vilkåret)
            .medKantIKantVurderer(kantIKantVurderer)
            .medType(vilkårType);
        if (fullstendigTidslinje != null) {
            vilkårBuilder.medFullstendigTidslinje(fullstendigTidslinje);
        }
        return vilkårBuilder;
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
        if (fullstendigTidslinje != null) {
            vilkårBuilder.medFullstendigTidslinje(fullstendigTidslinje);
        }
        kladd.leggTilVilkår(vilkårBuilder.build());
        return this;
    }

    /**
     * OBS: Returnerer alltid nytt vilkårresultat.
     */
    public Vilkårene build() {
        if (built)
            throw new IllegalStateException("Kan ikke bygge to ganger med samme builder");

        if (!kappOverskytende) {
            validerPerioder();
            built = true;
            return kladd;
        } else {
            var ny = new Vilkårene(kladd.getVilkårene(), boundry);
            built = true;
            return ny;
        }

    }

    private void validerPerioder() {
        var invalidVilkårMap = new HashMap<VilkårType, List<VilkårPeriode>>();

        if (!kappOverskytende) {
            kladd.getVilkårene().forEach(v -> invalidVilkårMap.put(v.getVilkårType(), v.getPerioder().stream().filter(it -> gårUtenforBoundry(it.getPeriode())).collect(Collectors.toList())));
            var harPerioderSomGårUtoverGrensen = invalidVilkårMap.entrySet().stream().anyMatch(it -> !it.getValue().isEmpty());
            if (harPerioderSomGårUtoverGrensen) {
                log.warn("Behandligen har vilkår med perioder[{}] som strekker seg utover maks({}) for fagsaken", invalidVilkårMap, boundry);
                throw new IllegalStateException("Behandligen har perioder som strekker seg utover maks(" + boundry + ") for fagsaken: " + invalidVilkårMap);
            }
        }
    }

    private boolean gårUtenforBoundry(DatoIntervallEntitet periode) {
        if (boundry.getFomDato().isAfter(periode.getFomDato())) {
            return true;
        }
        return boundry.getTomDato().isBefore(periode.getTomDato());
    }

    public VilkårResultatBuilder leggTilIkkeVurderteVilkår(List<DatoIntervallEntitet> intervaller, List<VilkårType> vilkår) {
        vilkår.stream()
            .map(type -> hentBuilderFor(type)
                .medType(type)
                .medMaksMellomliggendePeriodeAvstand(mellomliggendePeriodeAvstand)
                .medKantIKantVurderer(kantIKantVurderer))
            .peek(v -> intervaller.forEach(p -> v.leggTil(v.hentBuilderFor(p.getFomDato(), p.getTomDato()).medUtfall(Utfall.IKKE_VURDERT))))
            .forEach(this::leggTil);
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
            leggTil(builder);
        });
        return this;
    }

    public VilkårResultatBuilder medBoundry(DatoIntervallEntitet periode, boolean kappOverskytende) {
        this.boundry = new LocalDateInterval(periode.getFomDato(), periode.getTomDato());
        this.kappOverskytende = kappOverskytende;
        return this;
    }

    public VilkårResultatBuilder medFullstendigTidslinje(VilkårBuilder vilkårBuilder) {
        if (vilkårBuilder != null) {
            this.fullstendigTidslinje = vilkårBuilder.getTidslinje();
        } else {
            this.fullstendigTidslinje = null;
        }
        return this;
    }

    public void slettPerioder(VilkårType vilkårType, Collection<DatoIntervallEntitet> perioder) {

        List<LocalDateSegment<Boolean>> segmenter = perioder.stream().map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), Boolean.TRUE)).collect(Collectors.toList());
        LocalDateTimeline<Boolean> slettTimeline = new LocalDateTimeline<>(segmenter);

        slettPerioder(vilkårType, slettTimeline);
    }

    private void slettPerioder(VilkårType vilkårType, LocalDateTimeline<Boolean> slettTimeline) {
        var vilkåropt = kladd.getVilkår(vilkårType);
        if (vilkåropt.isEmpty()) {
            return;
        }
        var vilkår = vilkåropt.get();
        var eksisterendeTimeline = kladd.getVilkårTimeline(vilkårType);
        if (eksisterendeTimeline.isEmpty()) {
            return;
        }

        var nyTimeline = eksisterendeTimeline.disjoint(slettTimeline, (iv, s1, s2) -> {
            var nyVilkårPeriode = new VilkårPeriodeBuilder(s1.getValue()).medPeriode(iv.getFomDato(), iv.getTomDato()).build();
            return new LocalDateSegment<>(iv, nyVilkårPeriode);
        });

        var vilkårPerioder = nyTimeline.toSegments().stream().map(LocalDateSegment::getValue).collect(Collectors.toList());
        vilkår.setPerioder(vilkårPerioder);
    }

    public void slettVilkårPerioder(VilkårType vilkårType, DatoIntervallEntitet periode) {
        var slettTimeline = new LocalDateTimeline<>(new LocalDateInterval(periode.getFomDato(), periode.getTomDato()), Boolean.TRUE);
        slettPerioder(vilkårType, slettTimeline);
    }

    public KantIKantVurderer getKantIKantVurderer() {
        return kantIKantVurderer;
    }
}
