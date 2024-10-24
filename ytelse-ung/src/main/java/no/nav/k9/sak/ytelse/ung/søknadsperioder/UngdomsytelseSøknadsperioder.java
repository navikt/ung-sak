package no.nav.k9.sak.ytelse.ung.søknadsperioder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "UngdomsytelseSøknadsperioder")
@Table(name = "UNG_SOEKNADSPERIODER")
@Immutable
public class UngdomsytelseSøknadsperioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_SOEKNADSPERIODER")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<UngdomsytelseSøknadsperiode> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseSøknadsperioder() {
        // hibernate
    }

    public UngdomsytelseSøknadsperioder(UngdomsytelseSøknadsperioder periode) {
        this.journalpostId = periode.getJournalpostId();
        this.perioder = periode.getPerioder()
            .stream()
            .map(UngdomsytelseSøknadsperiode::new)
            .collect(Collectors.toSet());
        // hibernate
    }

    public UngdomsytelseSøknadsperioder(JournalpostId journalpostId, UngdomsytelseSøknadsperiode... perioder) {
        this(journalpostId, Arrays.asList(perioder));
    }

    public UngdomsytelseSøknadsperioder(JournalpostId journalpostId, Collection<UngdomsytelseSøknadsperiode> perioder) {
        this.journalpostId = journalpostId;
        this.perioder = new LinkedHashSet<>(Objects.requireNonNull(perioder));
    }

    public Long getId() {
        return id;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Set<UngdomsytelseSøknadsperiode> getPerioder() {
        return Collections.unmodifiableSet(perioder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsytelseSøknadsperioder that = (UngdomsytelseSøknadsperioder) o;
        return Objects.equals(journalpostId, that.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId);
    }

    @Override
    public String toString() {
        return "Søknadsperioder{" +
            "journalpostId=" + journalpostId +
            ", perioder=" + perioder +
            '}';
    }
}
