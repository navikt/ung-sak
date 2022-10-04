package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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

@Entity(name = "VurdertInstitusjonHolder")
@Table(name = "olp_vurdert_institusjon_holder")
@Immutable
public class VurdertInstitusjonHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_INSTITUSJON_HOLDER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<VurdertInstitusjon> vurdertInstitusjon = new LinkedHashSet<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VurdertInstitusjonHolder() {
    }

    public VurdertInstitusjonHolder(List<VurdertInstitusjon> vurdertInstitusjon) {
        this.vurdertInstitusjon = new LinkedHashSet<>(vurdertInstitusjon);
    }

    public List<VurdertInstitusjon> getVurdertInstitusjon() {
        return vurdertInstitusjon.stream().toList();
    }

    public Optional<VurdertInstitusjon> finnVurdertInstitusjon(String institusjon) {
        return vurdertInstitusjon.stream().filter(vi -> vi.getInstitusjon().equals(institusjon)).findFirst();
    }
}
