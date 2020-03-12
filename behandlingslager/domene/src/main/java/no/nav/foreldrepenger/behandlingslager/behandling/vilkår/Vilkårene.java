package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import static java.util.stream.Collectors.joining;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;

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
    @OneToMany(cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE }, mappedBy = "vilkårene")
    private Set<Vilkår> vilkårne = new LinkedHashSet<>();

    Vilkårene() {
        // for hibernate
    }

    Vilkårene(Vilkårene resultat) {
        if (resultat != null) {
            // For kopi mellom behandlinger
            this.vilkårne = resultat.vilkårne.stream().map(Vilkår::new).peek(v -> v.setVilkårene(this)).collect(Collectors.toSet());
        }
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
        vilkår.setVilkårene(this);
        vilkårne.add(vilkår);
    }

    void fjernVilkår(VilkårType vilkårType) {
        vilkårne.removeIf(it -> it.getVilkårType().equals(vilkårType));
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

    public VilkårResultatType getResultatType() {
        if (getHarVilkårTilVurdering()) {
            return VilkårResultatType.IKKE_FASTSATT;
        } else if (getHarAvslåtteVilkårsPerioder()) {
            final var utfall = getVilkårene()
                .stream()
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .map(VilkårPeriode::getGjeldendeUtfall)
                .collect(Collectors.toSet());
            if (utfall.containsAll(Set.of(Utfall.IKKE_OPPFYLT, Utfall.OPPFYLT))) {
                return VilkårResultatType.DELEVIS_AVSLÅTT;
            } else if (Set.of(Utfall.IKKE_OPPFYLT).equals(utfall)) {
                return VilkårResultatType.AVSLÅTT;
            }
        }
        return VilkårResultatType.INNVILGET;
    }

    private boolean getHarVilkårTilVurdering() {
        return getVilkårene()
            .stream()
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .anyMatch(vp -> Utfall.IKKE_VURDERT.equals(vp.getGjeldendeUtfall()));
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
    private LocalDateTimeline<VilkårPeriode> getVilkårTimeline(VilkårType vilkårType) {
        var vilkår = getVilkår(vilkårType);

        if (vilkår.isEmpty()) {
            return LocalDateTimeline.EMPTY_TIMELINE;
        } else {
            return new LocalDateTimeline<VilkårPeriode>(vilkår.get().getPerioder().stream()
                .map(v -> new LocalDateSegment<VilkårPeriode>(v.getFom(), v.getTom(), v)).collect(Collectors.toList()));
        }
    }

    public Map<VilkårType, LocalDateTimeline<VilkårPeriode>> getVilkårTidslinjer(DatoIntervallEntitet maksPeriode) {
        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> map = new EnumMap<>(VilkårType.class);
        vilkårne.stream().forEach(v -> map.put(v.getVilkårType(), getVilkårTimeline(v.getVilkårType(), maksPeriode.getFomDato(), maksPeriode.getTomDato())));
        return map;
    }
}
