package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private List<AktivitetsvilkårResultatPeriode> vurderinger = new ArrayList<>();

    public AktivitetsvilkårResultatHolder() {
    }

    public AktivitetsvilkårResultatHolder(List<AktivitetsvilkårResultatPeriode> vurderinger) {
        this.vurderinger = new ArrayList<>(vurderinger);
    }

    public Long getId() {
        return id;
    }

    public List<AktivitetsvilkårResultatPeriode> getVurderinger() {
        return Collections.unmodifiableList(vurderinger);
    }
}
