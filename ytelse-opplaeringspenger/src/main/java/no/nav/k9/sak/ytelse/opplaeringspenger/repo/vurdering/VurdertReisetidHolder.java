package no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "VurdertReisetidHolder")
@Table(name = "olp_vurdert_reisetid_holder")
@Immutable
public class VurdertReisetidHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_REISETID_HOLDER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<VurdertReisetid> reisetid;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertReisetidHolder() {
    }

    public VurdertReisetidHolder(List<VurdertReisetid> reisetid) {
        Objects.requireNonNull(reisetid);
        this.reisetid = reisetid.stream()
            .map(VurdertReisetid::new)
            .collect(Collectors.toSet());
    }

    public List<VurdertReisetid> getReisetid() {
        return reisetid.stream().toList();
    }
}
