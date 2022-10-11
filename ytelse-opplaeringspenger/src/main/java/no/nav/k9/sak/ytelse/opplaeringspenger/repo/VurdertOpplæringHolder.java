package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "VurdertOpplæringHolder")
@Table(name = "olp_vurdert_opplaering_holder")
@Immutable
public class VurdertOpplæringHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_OPPLAERING_HOLDER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<VurdertOpplæring> vurdertOpplæring = new LinkedHashSet<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VurdertOpplæringHolder() {
    }

    public VurdertOpplæringHolder(List<VurdertOpplæring> vurdertOpplæring) {
        this.vurdertOpplæring = new LinkedHashSet<>(vurdertOpplæring);
    }

    public List<VurdertOpplæring> getVurdertOpplæring() {
        return vurdertOpplæring.stream().toList();
    }
}
