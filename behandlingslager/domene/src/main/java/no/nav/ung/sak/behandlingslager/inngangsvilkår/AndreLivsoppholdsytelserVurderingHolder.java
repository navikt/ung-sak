package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity(name = "AndreLivsoppholdsytelserVurderingHolder")
@Table(name = "livsopphold_vurd_holder")
public class AndreLivsoppholdsytelserVurderingHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LIVSOPPHOLD_VURD_HOLDER")
    @SequenceGenerator(name = "SEQ_LIVSOPPHOLD_VURD_HOLDER", sequenceName = "seq_livsopphold_vurd_holder", allocationSize = 50)
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "livsopphold_vurd_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AndreLivsoppholdsytelserVurderingPeriode> vurderinger = new ArrayList<>();

    public AndreLivsoppholdsytelserVurderingHolder() {
    }

    public AndreLivsoppholdsytelserVurderingHolder(List<AndreLivsoppholdsytelserVurderingPeriode> vurderinger) {
        this.vurderinger = new ArrayList<>(vurderinger);
    }

    public Long getId() {
        return id;
    }

    public List<AndreLivsoppholdsytelserVurderingPeriode> getVurderinger() {
        return Collections.unmodifiableList(vurderinger);
    }
}
