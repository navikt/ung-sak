package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.*;
import java.util.stream.Collectors;

@Entity(name = "AktivitetsvilkårResultatHolder")
@Table(name = "aktivitet_resultat_holder")
class AktivitetsvilkårResultatHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AKTIVITET_RESULTAT_HOLDER")
    @SequenceGenerator(name = "SEQ_AKTIVITET_RESULTAT_HOLDER", sequenceName = "seq_aktivitet_resultat_holder", allocationSize = 50)
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "aktivitet_resultat_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AktivitetsvilkårResultatPeriode> vurderinger = new HashSet<>();

    public AktivitetsvilkårResultatHolder() {
    }

    public AktivitetsvilkårResultatHolder(Collection<AktivitetsvilkårResultatPeriode> vurderinger) {
        this.vurderinger = vurderinger.stream().map(AktivitetsvilkårResultatPeriode::new).collect(Collectors.toSet());
    }

    public Long getId() {
        return id;
    }

    public Set<AktivitetsvilkårResultatPeriode> getVurderinger() {
        return Collections.unmodifiableSet(vurderinger);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AktivitetsvilkårResultatHolder that = (AktivitetsvilkårResultatHolder) o;
        return Objects.equals(vurderinger, that.vurderinger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vurderinger);
    }
}
