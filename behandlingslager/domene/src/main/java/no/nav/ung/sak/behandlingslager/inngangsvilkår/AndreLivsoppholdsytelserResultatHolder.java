package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.*;
import java.util.stream.Collectors;

@Entity(name = "AndreLivsoppholdsytelserResultatHolder")
@Table(name = "livsopphold_resultat_holder")
public class AndreLivsoppholdsytelserResultatHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LIVSOPPHOLD_RESULTAT_HOLDER")
    @SequenceGenerator(name = "SEQ_LIVSOPPHOLD_RESULTAT_HOLDER", sequenceName = "seq_livsopphold_resultat_holder", allocationSize = 50)
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "livsopphold_resultat_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AndreLivsoppholdsytelserResultatPeriode> vurderinger = new HashSet<>();

    public AndreLivsoppholdsytelserResultatHolder() {
    }

    public AndreLivsoppholdsytelserResultatHolder(Collection<AndreLivsoppholdsytelserResultatPeriode> vurderinger) {
        this.vurderinger = vurderinger.stream().map(AndreLivsoppholdsytelserResultatPeriode::new).collect(Collectors.toSet());
    }

    public Long getId() {
        return id;
    }

    public Set<AndreLivsoppholdsytelserResultatPeriode> getVurderinger() {
        return Collections.unmodifiableSet(vurderinger);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AndreLivsoppholdsytelserResultatHolder annen
            && Objects.equals(vurderinger, annen.vurderinger);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(vurderinger);
    }
}
