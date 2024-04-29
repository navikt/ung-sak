package no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering;

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

@Entity(name = "VurdertNødvendighetHolder")
@Table(name = "olp_vurdert_noedvendighet_holder")
@Immutable
public class VurdertNødvendighetHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_NOEDVENDIGHET_HOLDER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<VurdertNødvendighet> vurdertNødvendighet;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertNødvendighetHolder() {
    }

    public VurdertNødvendighetHolder(List<VurdertNødvendighet> vurdertNødvendighet) {
        Objects.requireNonNull(vurdertNødvendighet);
        this.vurdertNødvendighet = vurdertNødvendighet.stream()
            .map(VurdertNødvendighet::new)
            .collect(Collectors.toSet());
    }

    public List<VurdertNødvendighet> getVurdertNødvendighet() {
        return vurdertNødvendighet.stream().toList();
    }

    public Optional<VurdertNødvendighet> finnVurderingForJournalpostId(JournalpostId journalpostId) {
        return vurdertNødvendighet.stream().filter(vo -> vo.getJournalpostId().equals(journalpostId)).findFirst();
    }
}
