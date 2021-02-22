package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "PerioderFraSøknad")
@Table(name = "UP_SOEKNAD_PERIODER")
@Immutable
public class PerioderFraSøknad extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_SOEKNAD_PERIODER")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<ArbeidPeriode> arbeidPerioder;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<UttakPeriode> uttakPerioder;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<Tilsynsordning> tilsynsordning;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<FeriePeriode> ferie;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    PerioderFraSøknad() {
        // hibernate
    }

    PerioderFraSøknad(PerioderFraSøknad periode) {
        this.journalpostId = periode.getJournalpostId();
        this.arbeidPerioder = periode.getArbeidPerioder()
            .stream()
            .map(ArbeidPeriode::new)
            .collect(Collectors.toSet());
        this.uttakPerioder = periode.getUttakPerioder()
            .stream()
            .map(UttakPeriode::new)
            .collect(Collectors.toSet());
        this.tilsynsordning = periode.getTilsynsordning()
            .stream()
            .map(Tilsynsordning::new)
            .collect(Collectors.toSet());
        this.ferie = periode.getFerie()
            .stream()
            .map(FeriePeriode::new)
            .collect(Collectors.toSet());
    }

    public PerioderFraSøknad(JournalpostId journalpostId,
                             Collection<UttakPeriode> uttakPerioder,
                             Collection<ArbeidPeriode> arbeidPerioder,
                             Collection<Tilsynsordning> tilsynsorning,
                             Collection<FeriePeriode> ferie) {
        this.journalpostId = journalpostId;
        this.uttakPerioder = new LinkedHashSet<>(Objects.requireNonNull(uttakPerioder));
        this.arbeidPerioder = new LinkedHashSet<>(Objects.requireNonNull(arbeidPerioder));
        this.tilsynsordning = new LinkedHashSet<>(Objects.requireNonNull(tilsynsorning));
        this.ferie = new LinkedHashSet<>(Objects.requireNonNull(ferie));
    }

    public Long getId() {
        return id;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Set<ArbeidPeriode> getArbeidPerioder() {
        return Collections.unmodifiableSet(arbeidPerioder);
    }

    public Set<UttakPeriode> getUttakPerioder() {
        return Collections.unmodifiableSet(uttakPerioder);
    }

    public Set<Tilsynsordning> getTilsynsordning() {
        return Collections.unmodifiableSet(tilsynsordning);
    }

    public Set<FeriePeriode> getFerie() {
        return Collections.unmodifiableSet(ferie);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerioderFraSøknad that = (PerioderFraSøknad) o;
        return Objects.equals(arbeidPerioder, that.arbeidPerioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidPerioder);
    }

    @Override
    public String toString() {
        return "Søknadsperioder{" +
            "journalpostId=" + journalpostId +
            '}';
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(journalpostId);
    }
}
