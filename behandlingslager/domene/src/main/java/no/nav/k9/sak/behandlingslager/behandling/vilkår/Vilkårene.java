package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import static java.util.stream.Collectors.joining;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.BatchSize;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "Vilkårene")
@Table(name = "VR_VILKAR_RESULTAT")
public class Vilkårene extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VILKAR_RESULTAT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    // CascadeType.ALL + orphanRemoval=true må til for at Vilkår skal bli slettet fra databasen ved fjerning fra HashSet
    @OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true)
    @JoinColumn(name = "vilkar_resultat_id", nullable = false)
    @BatchSize(size = 20)
    private Set<Vilkår> vilkårne = new HashSet<>();

    Vilkårene() {
        // for hibernate
    }

    Vilkårene(Vilkårene resultat) {
        if (resultat != null) {
            // For kopi mellom behandlinger
            this.vilkårne = resultat.vilkårne.stream().map(Vilkår::new).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    /** Oppretter vilkårne fra eksisternede, men med avkapping ifht. angitt boundry. */
    Vilkårene(Collection<Vilkår> vilkår, LocalDateInterval boundry) {
        this.vilkårne = vilkår.stream().map(v -> new VilkårBuilder(v, boundry).build()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static VilkårResultatBuilder builder() {
        return new VilkårResultatBuilder();
    }

    public static VilkårResultatBuilder builderFraEksisterende(Vilkårene eksisterendeResultat) {
        return new VilkårResultatBuilder(eksisterendeResultat);
    }

    public Long getId() {
        return id;
    }

    /**
     * Returnerer kopi liste av vilkår slik at denne ikke kan modifiseres direkte av klåfingrede utviklere.
     */
    public List<Vilkår> getVilkårene() {
        return List.copyOf(vilkårne);
    }

    void leggTilVilkår(Vilkår vilkår) {
        vilkårne.removeIf(it -> it.getVilkårType().equals(vilkår.getVilkårType()));
        vilkårne.add(vilkår);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id=" + id +
            ", versjon=" + versjon +
            ", vilkårne=" + vilkårne +
            ", vilkår={" + vilkårne.stream().map(Vilkår::toString).collect(joining("},{")) + "}" +
            '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Vilkårene)) {
            return false;
        }
        Vilkårene other = (Vilkårene) obj;
        return Objects.equals(vilkårne, other.vilkårne);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVilkårene());
    }

    public boolean getHarAvslåtteVilkårsPerioder() {
        return getVilkårene()
            .stream()
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .anyMatch(vp -> Utfall.IKKE_OPPFYLT.equals(vp.getGjeldendeUtfall()));
    }

    public Optional<Vilkår> getVilkår(VilkårType vilkårType) {
        return vilkårne.stream().filter(v -> Objects.equals(vilkårType, v.getVilkårType())).findAny();
    }

    public LocalDateTimeline<VilkårPeriode> getVilkårTimeline(VilkårType vilkårType, LocalDate fom, LocalDate tom) {
        return getVilkårTimeline(vilkårType).intersection(new LocalDateInterval(fom, tom));
    }

    @SuppressWarnings("unchecked")
    public LocalDateTimeline<VilkårPeriode> getVilkårTimeline(VilkårType vilkårType) {
        var vilkår = getVilkår(vilkårType);

        if (vilkår.isEmpty()) {
            return LocalDateTimeline.EMPTY_TIMELINE;
        } else {
            return new LocalDateTimeline<>(vilkår.get().getPerioder().stream()
                .map(v -> new LocalDateSegment<>(v.getFom(), v.getTom(), v)).collect(Collectors.toList()));
        }
    }

    public Map<VilkårType, LocalDateTimeline<VilkårPeriode>> getVilkårTidslinjer(DatoIntervallEntitet maksPeriode) {
        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> map = new EnumMap<>(VilkårType.class);
        vilkårne.forEach(v -> map.put(v.getVilkårType(), getVilkårTimeline(v.getVilkårType(), maksPeriode.getFomDato(), maksPeriode.getTomDato())));
        return map;
    }

    public Map<VilkårType, Set<Avslagsårsak>> getVilkårMedAvslagsårsaker() {
        Map<VilkårType, Set<Avslagsårsak>> result = new EnumMap<>(VilkårType.class);
        for (Vilkår vilkår : vilkårne) {
            var avslagsårsaker = vilkår.getPerioder().stream()
                .map(VilkårPeriode::getAvslagsårsak)
                .filter(Objects::nonNull)
                .filter(it -> !Avslagsårsak.UDEFINERT.equals(it))
                .collect(Collectors.toSet());
            if (!avslagsårsaker.isEmpty()) {
                result.put(vilkår.getVilkårType(), avslagsårsaker);
            }
        }
        return result;
    }

    public LocalDateTimeline<Boolean> getAlleIntervaller() {
        // returnere alle intervaller (ukomprimert)
        var segmenter = getVilkårene().stream()
            .flatMap(v -> v.getPerioder().stream())
            .map(v -> v.getPeriode().toLocalDateInterval())
            .map(iv -> new LocalDateSegment<>(iv, true))
            .toList();
        return new LocalDateTimeline<>(segmenter, (iv, v1, v2) -> new LocalDateSegment<>(iv, true));
    }

    public LocalDateTimeline<List<Resultat>> getAlleVilkårTidslinjeResultater(DatoIntervallEntitet maksPeriode) {
        var vilkårTidslinjer = getVilkårTidslinjer(maksPeriode);
        LocalDateTimeline<List<Resultat>> resultat = new LocalDateTimeline<>(Set.of());

        for (var entry : vilkårTidslinjer.entrySet()) {
            var type = entry.getKey();
            LocalDateTimeline<Resultat> tidslinje = entry.getValue().mapValue(vp -> new Resultat(type, vp.getGjeldendeUtfall()));
            resultat = resultat.crossJoin(tidslinje, StandardCombinators::allValues);
        }
        return resultat;
    }

    public record Resultat(VilkårType type, Utfall utfall) {
    }
}
