package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.typer.JournalpostId;

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
    private Set<VurdertInstitusjon> vurdertInstitusjon;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertInstitusjonHolder() {
    }

    public VurdertInstitusjonHolder(List<VurdertInstitusjon> vurdertInstitusjon) {
        Objects.requireNonNull(vurdertInstitusjon);
        this.vurdertInstitusjon = vurdertInstitusjon.stream()
            .map(VurdertInstitusjon::new)
            .collect(Collectors.toSet());
    }

    public List<VurdertInstitusjon> getVurdertInstitusjon() {
        return vurdertInstitusjon.stream().toList();
    }

    public Optional<VurdertInstitusjon> finnVurderingForJournalpostId(JournalpostId journalpostId) {
        return vurdertInstitusjon.stream().filter(vi -> vi.getJournalpostId().equals(journalpostId)).findFirst();
    }
}
