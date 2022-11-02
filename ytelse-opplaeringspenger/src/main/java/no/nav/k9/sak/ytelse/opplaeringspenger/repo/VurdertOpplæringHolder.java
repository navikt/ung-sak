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
    private Set<VurdertOpplæring> vurdertOpplæring;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertOpplæringHolder() {
    }

    public VurdertOpplæringHolder(List<VurdertOpplæring> vurdertOpplæring) {
        Objects.requireNonNull(vurdertOpplæring);
        this.vurdertOpplæring = vurdertOpplæring.stream()
            .map(VurdertOpplæring::new)
            .collect(Collectors.toSet());
    }

    public List<VurdertOpplæring> getVurdertOpplæring() {
        return vurdertOpplæring.stream().toList();
    }

    public Optional<VurdertOpplæring> finnVurderingForJournalpostId(JournalpostId journalpostId) {
        return vurdertOpplæring.stream().filter(vo -> vo.getJournalpostId().equals(journalpostId)).findFirst();
    }
}
