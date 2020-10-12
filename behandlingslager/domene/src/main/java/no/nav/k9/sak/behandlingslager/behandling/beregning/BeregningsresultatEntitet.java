package no.nav.k9.sak.behandlingslager.behandling.beregning;

import java.sql.Clob;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

@Entity(name = "Beregningsresultat")
@Table(name = "BR_BEREGNINGSRESULTAT")
@DynamicInsert
@DynamicUpdate
public class BeregningsresultatEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_BEREGNINGSRESULTAT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsresultat", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<BeregningsresultatPeriode> beregningsresultatPerioder = new ArrayList<>();

    /**
     * Er egentlig OneToOne, men må mappes slik da JPA/Hibernate ikke støtter OneToOne på annet enn shared PK.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsresultat", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private Set<BeregningsresultatFeriepenger> beregningsresultatFeriepenger = new HashSet<>(1);

    @Lob
    @Column(name = "regel_input", nullable = false)
    @DiffIgnore
    private Clob regelInput;

    @Lob
    @Column(name = "regel_sporing", nullable = false)
    @DiffIgnore
    private Clob regelSporing;

    @Lob
    @Column(name = "feriepenger_regel_input")
    @DiffIgnore
    private Clob feriepengerRegelInput;

    @Lob
    @Column(name = "feriepenger_regel_sporing")
    @DiffIgnore
    private Clob feriepengerRegelSporing;

    @Column(name = "endringsdato")
    private LocalDate endringsdato;

    public Long getId() {
        return id;
    }

    public RegelData getRegelInput() {
        return regelInput == null ? null : new RegelData(regelInput);
    }

    public RegelData getRegelSporing() {
        return regelSporing == null ? null : new RegelData(regelSporing);
    }

    public RegelData getFeriepengerRegelInput() {
        return feriepengerRegelInput == null ? null : new RegelData(feriepengerRegelInput);
    }

    public RegelData getFeriepengerRegelSporing() {
        return feriepengerRegelSporing == null ? null : new RegelData(feriepengerRegelSporing);
    }

    @PrePersist
    protected void onBeregningsresultatCreate() {
        // kun mens vi migrerer
        if (getBeregningsresultatFeriepenger().isPresent()) {
            var br = getBeregningsresultatFeriepenger().get();
            if (feriepengerRegelInput == null) {
                feriepengerRegelInput = Optional.ofNullable(br.getFeriepengerRegelInput()).map(r -> r.clob).orElseGet(null);
            }
            if (feriepengerRegelSporing == null) {
                feriepengerRegelSporing = Optional.ofNullable(br.getFeriepengerRegelSporing()).map(r -> r.clob).orElseGet(null);
            }
        }
    }

    @PreUpdate
    protected void onBeregningsresultatUpdate() {
        // kun mens vi migrerer
        if (getBeregningsresultatFeriepenger().isPresent()) {
            var br = getBeregningsresultatFeriepenger().get();
            if (feriepengerRegelInput == null) {
                feriepengerRegelInput = Optional.ofNullable(br.getFeriepengerRegelInput()).map(r -> r.clob).orElseGet(null);
            }
            if (feriepengerRegelSporing == null) {
                feriepengerRegelSporing = Optional.ofNullable(br.getFeriepengerRegelSporing()).map(r -> r.clob).orElseGet(null);
            }
        }

    }


    public Optional<LocalDate> getEndringsdato() {
        return Optional.ofNullable(endringsdato);
    }

    public List<BeregningsresultatPeriode> getBeregningsresultatPerioder() {
        return Collections.unmodifiableList(beregningsresultatPerioder);
    }

    public LocalDateTimeline<List<BeregningsresultatAndel>> getBeregningsresultatAndelTimeline() {
        var perioder = getBeregningsresultatPerioder().stream()
            .map(v -> new LocalDateSegment<>(v.getBeregningsresultatPeriodeFom(), v.getBeregningsresultatPeriodeTom(), v.getBeregningsresultatAndelList()))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(perioder);
    }

    public List<BeregningsresultatFeriepengerPrÅr> getBeregningsresultatFeriepengerPrÅrListe() {
        return List.copyOf(getBeregningsresultatAndelTimeline().toSegments().stream()
            .flatMap(s -> s.getValue().stream())
            .flatMap(a -> a.getBeregningsresultatFeriepengerPrÅrListe().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /** @deprecated Bruk {@link #getBeregningsresultatAndelTimeline()} i stedet. */
    @Deprecated(forRemoval = true)
    public LocalDateTimeline<BeregningsresultatPeriode> getBeregningsresultatTimeline() {
        var perioder = getBeregningsresultatPerioder().stream()
            .map(v -> new LocalDateSegment<>(v.getBeregningsresultatPeriodeFom(), v.getBeregningsresultatPeriodeTom(), v))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(perioder);
    }

    public void addBeregningsresultatPeriode(BeregningsresultatPeriode brPeriode) {
        Objects.requireNonNull(brPeriode, "beregningsresultatPeriode");
        if (!beregningsresultatPerioder.contains(brPeriode)) { // NOSONAR Class defines List based fields but uses them like Sets: Ingening å tjene på å bytte til Set ettersom det er små lister
            beregningsresultatPerioder.add(brPeriode);
        }
    }

    public Optional<BeregningsresultatFeriepenger> getBeregningsresultatFeriepenger() {
        if (this.beregningsresultatFeriepenger.size() > 1) {
            throw new IllegalStateException("Utviklerfeil: Det finnes flere BeregningsresultatFeriepenger");
        }
        return beregningsresultatFeriepenger.isEmpty() ? Optional.empty() : Optional.of(beregningsresultatFeriepenger.iterator().next());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsresultatEntitet)) {
            return false;
        }
        BeregningsresultatEntitet other = (BeregningsresultatEntitet) obj;
        return Objects.equals(this.getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<perioder=" + beregningsresultatPerioder + ", feriepenger=" + beregningsresultatFeriepenger + ">";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsresultatEntitet beregningsresultat) {
        return new Builder(beregningsresultat);
    }

    public static class Builder {
        private BeregningsresultatEntitet mal;

        Builder() {
            this.mal = new BeregningsresultatEntitet();
        }

        Builder(BeregningsresultatEntitet beregningsresultat) {
            this.mal = beregningsresultat;
        }

        public Builder medRegelInput(String regelInput) {
            if (regelInput == null || regelInput.isBlank()) {
                throw new IllegalArgumentException("kan ikke ha null eller empty string");
            }
            mal.regelInput = RegelData.createProxy(regelInput);
            return this;
        }

        public Builder medRegelSporing(String regelSporing) {
            if (regelSporing == null || regelSporing.isBlank()) {
                throw new IllegalArgumentException("kan ikke ha null eller empty string");
            }
            mal.regelSporing = RegelData.createProxy(regelSporing);
            return this;
        }

        public Builder medRegelInput(RegelData regelInput) {
            mal.regelInput = regelInput == null ? null : regelInput.clob;
            return this;
        }

        public Builder medRegelSporing(RegelData regelSporing) {
            mal.regelSporing = regelSporing == null ? null : regelSporing.clob;
            return this;
        }

        public Builder medFeriepengerRegelInput(String regelInput) {
            mal.feriepengerRegelInput = RegelData.createProxy(regelInput);
            return this;
        }

        public Builder medFeriepengerRegelSporing(String regelSporing) {
            mal.feriepengerRegelSporing = RegelData.createProxy(regelSporing);
            return this;
        }

        public Builder medFeriepengerRegelInput(RegelData regelInput) {
            mal.feriepengerRegelInput = regelInput == null ? null : regelInput.clob;
            return this;
        }

        public Builder medFeriepengerRegelSporing(RegelData regelSporing) {
            mal.feriepengerRegelSporing = regelSporing == null ? null : regelSporing.clob;
            return this;
        }

        public Builder medBeregningsresultatFeriepenger(BeregningsresultatFeriepenger beregningsresultatFeriepenger) {
            mal.beregningsresultatFeriepenger.clear();
            mal.beregningsresultatFeriepenger.add(beregningsresultatFeriepenger);

            medFeriepengerRegelInput(beregningsresultatFeriepenger.getFeriepengerRegelInput());
            medFeriepengerRegelSporing(beregningsresultatFeriepenger.getFeriepengerRegelSporing());
            return this;
        }

        public Builder medEndringsdato(LocalDate endringsdato) {
            mal.endringsdato = endringsdato;
            return this;
        }

        public BeregningsresultatEntitet build() {
            verifyStateForBuild();
            return mal;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(mal.beregningsresultatPerioder, "beregningsresultatPerioder");
            Objects.requireNonNull(mal.regelInput, "regelInput");
            Objects.requireNonNull(mal.regelSporing, "regelSporing");
        }
    }

}
