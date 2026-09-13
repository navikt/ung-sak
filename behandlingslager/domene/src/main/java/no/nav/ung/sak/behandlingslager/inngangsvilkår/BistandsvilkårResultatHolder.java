package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.*;
import java.util.stream.Collectors;

@Entity(name = "BistandsvilkårResultatHolder")
@Table(name = "bistand_resultat_holder")
public class BistandsvilkårResultatHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BISTAND_RESULTAT_HOLDER")
    @SequenceGenerator(name = "SEQ_BISTAND_RESULTAT_HOLDER", sequenceName = "seq_bistand_resultat_holder", allocationSize = 50)
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "bistand_resultat_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BistandsvilkårResultatPeriode> vurderinger = new HashSet<>();

    public BistandsvilkårResultatHolder() {
    }

    public BistandsvilkårResultatHolder(Collection<BistandsvilkårResultatPeriode> vurderinger) {
        this.vurderinger = vurderinger.stream().map(BistandsvilkårResultatPeriode::new).collect(Collectors.toSet());
    }

    public Long getId() {
        return id;
    }

    public Set<BistandsvilkårResultatPeriode> getVurderinger() {
        return Collections.unmodifiableSet(vurderinger);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BistandsvilkårResultatHolder annen
            && Objects.equals(vurderinger, annen.vurderinger);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(vurderinger);
    }
}
