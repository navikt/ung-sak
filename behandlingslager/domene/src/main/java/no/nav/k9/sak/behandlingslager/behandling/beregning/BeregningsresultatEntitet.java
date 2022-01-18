package no.nav.k9.sak.behandlingslager.behandling.beregning;

import java.sql.Clob;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    public void setFeriepengerRegelInput(String data) {
        setFeriepengerRegelInput(data == null ? null : new RegelData(data));
    }

    public void setFeriepengerRegelInput(RegelData data) {
        if (this.feriepengerRegelInput != null) {
            throw new IllegalStateException("regelInput allerede satt, kan ikke sette på nytt: " + data);
        }
        this.feriepengerRegelInput = data == null ? null : data.getClob();
    }

    public void setFeriepengerRegelSporing(String data) {
        setFeriepengerRegelSporing(data == null ? null : new RegelData(data));
    }

    public void setFeriepengerRegelSporing(RegelData data) {
        if (this.feriepengerRegelSporing != null) {
            throw new IllegalStateException("regelSporing allerede satt, kan ikke sette på nytt: " + regelInput);
        }
        this.feriepengerRegelSporing = data == null ? null : data.getClob();
    }

    public void setRegelInput(String data) {
        setRegelInput(data == null ? null : new RegelData(data));
    }

    public void setRegelInput(RegelData data) {
        if (this.regelInput != null) {
            throw new IllegalStateException("regelInput allerede satt, kan ikke sette på nytt: " + data);
        }
        this.regelInput = data == null ? null : data.getClob();
    }

    public void setRegelSporing(String data) {
        setRegelSporing(data == null ? null : new RegelData(data));
    }

    public void setRegelSporing(RegelData data) {
        if (this.regelSporing != null) {
            throw new IllegalStateException("regelSporing allerede satt, kan ikke sette på nytt: " + data);
        }
        this.regelSporing = data == null ? null : data.getClob();
    }

    public Optional<LocalDate> getEndringsdato() {
        return Optional.ofNullable(endringsdato);
    }

    public List<BeregningsresultatPeriode> getBeregningsresultatPerioder() {
        return Collections.unmodifiableList(beregningsresultatPerioder);
    }

    public LocalDateTimeline<List<BeregningsresultatAndel>> getBeregningsresultatAndelTimeline() {
        List<BeregningsresultatPeriode> brPerioder = getBeregningsresultatPerioder();
        var perioder = brPerioder.stream()
            .map(v -> new LocalDateSegment<>(v.getBeregningsresultatPeriodeFom(), v.getBeregningsresultatPeriodeTom(), v.getBeregningsresultatAndelList()))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(perioder);
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
        return getClass().getSimpleName() + "<perioder=" + beregningsresultatPerioder + ">";
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
