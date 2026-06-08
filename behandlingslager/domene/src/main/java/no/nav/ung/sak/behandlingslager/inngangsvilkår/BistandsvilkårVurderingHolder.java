package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity(name = "BistandsvilkårVurderingHolder")
@Table(name = "bistand_vurd_holder")
public class BistandsvilkårVurderingHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BISTAND_VURD_HOLDER")
    @SequenceGenerator(name = "SEQ_BISTAND_VURD_HOLDER", sequenceName = "seq_bistand_vurd_holder", allocationSize = 50)
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "bistand_vurd_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BistandsvilkårVurderingPeriode> vurderinger = new ArrayList<>();

    public BistandsvilkårVurderingHolder() {
    }

    public BistandsvilkårVurderingHolder(List<BistandsvilkårVurderingPeriode> vurderinger) {
        this.vurderinger = new ArrayList<>(vurderinger);
    }

    public Long getId() {
        return id;
    }

    public List<BistandsvilkårVurderingPeriode> getVurderinger() {
        return Collections.unmodifiableList(vurderinger);
    }
}
