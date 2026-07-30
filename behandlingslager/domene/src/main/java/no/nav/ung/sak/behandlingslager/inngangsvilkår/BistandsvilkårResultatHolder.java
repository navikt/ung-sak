package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private List<BistandsvilkårResultatPeriode> vurderinger = new ArrayList<>();

    public BistandsvilkårResultatHolder() {
    }

    public BistandsvilkårResultatHolder(List<BistandsvilkårResultatPeriode> vurderinger) {
        this.vurderinger = new ArrayList<>(vurderinger);
    }

    public Long getId() {
        return id;
    }

    public List<BistandsvilkårResultatPeriode> getVurderinger() {
        return Collections.unmodifiableList(vurderinger);
    }
}
