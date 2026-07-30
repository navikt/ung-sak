package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private List<AndreLivsoppholdsytelserResultatPeriode> vurderinger = new ArrayList<>();

    public AndreLivsoppholdsytelserResultatHolder() {
    }

    public AndreLivsoppholdsytelserResultatHolder(List<AndreLivsoppholdsytelserResultatPeriode> vurderinger) {
        this.vurderinger = new ArrayList<>(vurderinger);
    }

    public Long getId() {
        return id;
    }

    public List<AndreLivsoppholdsytelserResultatPeriode> getVurderinger() {
        return Collections.unmodifiableList(vurderinger);
    }
}
