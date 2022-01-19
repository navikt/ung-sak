package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.LocalDate;
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

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "PerioderFraSøknad")
@Table(name = "UP_SOEKNAD_PERIODER")
@Immutable
public class PerioderFraSøknad extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_SOEKNAD_PERIODER")
    private Long id;

    @ChangeTracked
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
    private Set<UtenlandsoppholdPeriode> utenlandsopphold;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<FeriePeriode> ferie;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<BeredskapPeriode> beredskap;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<NattevåkPeriode> nattevåk;

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
        this.utenlandsopphold = periode.getUtenlandsopphold()
            .stream()
            .map(UtenlandsoppholdPeriode::new)
            .collect(Collectors.toSet());
        this.ferie = periode.getFerie()
            .stream()
            .map(FeriePeriode::new)
            .collect(Collectors.toSet());
        this.beredskap = periode.getBeredskap()
                .stream()
                .map(BeredskapPeriode::new)
                .collect(Collectors.toSet());
        this.nattevåk = periode.getNattevåk()
                .stream()
                .map(NattevåkPeriode::new)
                .collect(Collectors.toSet());
    }

    public PerioderFraSøknad(JournalpostId journalpostId,
                             Collection<UttakPeriode> uttakPerioder,
                             Collection<ArbeidPeriode> arbeidPerioder,
                             Collection<Tilsynsordning> tilsynsorning,
                             Collection<UtenlandsoppholdPeriode> utenlandsopphold,
                             Collection<FeriePeriode> ferie,
                             Collection<BeredskapPeriode> beredskap,
                             Collection<NattevåkPeriode> nattevåk) {
        this.journalpostId = journalpostId;
        this.uttakPerioder = new LinkedHashSet<>(Objects.requireNonNull(uttakPerioder));
        this.arbeidPerioder = new LinkedHashSet<>(Objects.requireNonNull(arbeidPerioder));
        this.tilsynsordning = new LinkedHashSet<>(Objects.requireNonNull(tilsynsorning));
        this.utenlandsopphold = new LinkedHashSet<>(Objects.requireNonNull(utenlandsopphold));
        this.ferie = new LinkedHashSet<>(Objects.requireNonNull(ferie));
        this.beredskap = new LinkedHashSet<>(Objects.requireNonNull(beredskap));
        this.nattevåk = new LinkedHashSet<>(Objects.requireNonNull(nattevåk));
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

    public Set<UtenlandsoppholdPeriode> getUtenlandsopphold() {
        return Collections.unmodifiableSet(utenlandsopphold);
    }

    public Set<BeredskapPeriode> getBeredskap() {
        return Collections.unmodifiableSet(beredskap);
    }

    public Set<NattevåkPeriode> getNattevåk() {
        return Collections.unmodifiableSet(nattevåk);
    }

    public DatoIntervallEntitet utledSøktPeriode() {
        var min = uttakPerioder.stream()
            .map(UttakPeriode::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .min(LocalDate::compareTo)
            .orElseThrow();
        var max = uttakPerioder.stream()
            .map(UttakPeriode::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo)
            .orElseThrow();
        return DatoIntervallEntitet.fraOgMedTilOgMed(min, max);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerioderFraSøknad that = (PerioderFraSøknad) o;
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
            '}';
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(journalpostId);
    }
}
