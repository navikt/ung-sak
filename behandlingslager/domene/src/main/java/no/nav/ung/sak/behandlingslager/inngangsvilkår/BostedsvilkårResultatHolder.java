package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity(name = "BostedsvilkårResultatHolder")
@Table(name = "bosted_resultat_holder")
public class BostedsvilkårResultatHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSTED_RESULTAT_HOLDER")
    @SequenceGenerator(name = "SEQ_BOSTED_RESULTAT_HOLDER", sequenceName = "seq_bosted_resultat_holder", allocationSize = 50)
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "bosted_resultat_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BostedsvilkårResultatPeriode> vurderinger = new ArrayList<>();

    public BostedsvilkårResultatHolder() {
    }

    public BostedsvilkårResultatHolder(List<BostedsvilkårResultatPeriode> vurderinger) {
        this.vurderinger = new ArrayList<>(vurderinger);
    }

    public Long getId() {
        return id;
    }

    public List<BostedsvilkårResultatPeriode> getVurderinger() {
        return Collections.unmodifiableList(vurderinger);
    }
}

