package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "Søknadsperioder")
@Table(name = "SP_SOEKNADSPERIODER")
@Immutable
public class Søknadsperioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SP_SOEKNADSPERIODER")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<Søknadsperiode> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Søknadsperioder() {
        // hibernate
    }

    public Søknadsperioder(Søknadsperioder periode) {
        this.journalpostId = periode.getJournalpostId();
        this.perioder = periode.getPerioder()
            .stream()
            .map(Søknadsperiode::new)
            .collect(Collectors.toSet());
        // hibernate
    }

    public Søknadsperioder(JournalpostId journalpostId, Søknadsperiode... perioder) {
        this(journalpostId, Arrays.asList(perioder));
    }

    public Søknadsperioder(JournalpostId journalpostId, Collection<Søknadsperiode> perioder) {
        this.journalpostId = journalpostId;
        this.perioder = new LinkedHashSet<>(Objects.requireNonNull(perioder));
    }

    public Long getId() {
        return id;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Set<Søknadsperiode> getPerioder() {
        return Collections.unmodifiableSet(perioder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Søknadsperioder that = (Søknadsperioder) o;
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
