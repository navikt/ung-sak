package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE}, mappedBy = "vilkårene")
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

    public ResultatType getResultatType() {
        if (getHarVilkårTilVurdering()) {
            return ResultatType.IKKE_FASTSATT;
        } else if (getHarAvslåtteVilkårsPerioder()) {
            final var utfall = getVilkårene()
                .stream()
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .map(VilkårPeriode::getGjeldendeUtfall)
                .collect(Collectors.toSet());
            if (utfall.containsAll(Set.of(Utfall.IKKE_OPPFYLT, Utfall.OPPFYLT))) {
                return ResultatType.DELEVIS_AVSLÅTT;
            } else if (Set.of(Utfall.IKKE_OPPFYLT).equals(utfall)) {
                return ResultatType.AVSLÅTT;
            }
        }
        return ResultatType.INNVILGET;
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
}
