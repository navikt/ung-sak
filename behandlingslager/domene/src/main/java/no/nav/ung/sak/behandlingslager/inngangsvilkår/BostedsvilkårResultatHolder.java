package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.*;
import java.util.stream.Collectors;

@Entity(name = "BostedsvilkårResultatHolder")
@Table(name = "bosted_resultat_holder")
class BostedsvilkårResultatHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSTED_RESULTAT_HOLDER")
    @SequenceGenerator(name = "SEQ_BOSTED_RESULTAT_HOLDER", sequenceName = "seq_bosted_resultat_holder", allocationSize = 50)
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "bosted_resultat_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BostedsvilkårResultatPeriode> vurderinger = new HashSet<>();

    public BostedsvilkårResultatHolder() {
    }

    public BostedsvilkårResultatHolder(Collection<BostedsvilkårResultatPeriode> vurderinger) {
        this.vurderinger = vurderinger.stream().map(BostedsvilkårResultatPeriode::new).collect(Collectors.toSet());
    }

    public Long getId() {
        return id;
    }

    Set<BostedsvilkårResultatPeriode> getVurderinger() {
        return Collections.unmodifiableSet(vurderinger);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BostedsvilkårResultatHolder annen
            && Objects.equals(vurderinger, annen.vurderinger);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(vurderinger);
    }
}

