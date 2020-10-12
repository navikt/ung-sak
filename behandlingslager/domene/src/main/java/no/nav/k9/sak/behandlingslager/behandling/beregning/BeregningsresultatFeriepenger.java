package no.nav.k9.sak.behandlingslager.behandling.beregning;

import java.sql.Clob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

@Entity(name = "BeregningsresultatFeriepenger")
@Table(name = "BR_FERIEPENGER")
@DynamicInsert
@DynamicUpdate
public class BeregningsresultatFeriepenger extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_FERIEPENGER")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "BEREGNINGSRESULTAT_FP_ID", nullable = false, updatable = false)
    private BeregningsresultatEntitet beregningsresultat;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsresultatFeriepenger", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<BeregningsresultatFeriepengerPrÅr> beregningsresultatFeriepengerPrÅrListe = new ArrayList<>();

    @Lob
    @Column(name = "feriepenger_regel_input")
    @DiffIgnore
    private Clob feriepengerRegelInput;

    @Lob
    @Column(name = "feriepenger_regel_sporing")
    @DiffIgnore
    private Clob feriepengerRegelSporing;

    public Long getId() {
        return id;
    }

    public RegelData getFeriepengerRegelInput() {
        return feriepengerRegelInput == null ? null : new RegelData(feriepengerRegelInput);
    }

    public RegelData getFeriepengerRegelSporing() {
        return feriepengerRegelSporing == null ? null : new RegelData(feriepengerRegelSporing);
    }

    public List<BeregningsresultatFeriepengerPrÅr> getBeregningsresultatFeriepengerPrÅrListe() {
        return Collections.unmodifiableList(beregningsresultatFeriepengerPrÅrListe);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsresultatFeriepenger)) {
            return false;
        }
        BeregningsresultatFeriepenger other = (BeregningsresultatFeriepenger) obj;
        return Objects.equals(this.getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + beregningsresultatFeriepengerPrÅrListe.size() + ">";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsresultatFeriepenger beregningsresultatFeriepenger) {
        return new Builder(beregningsresultatFeriepenger);
    }

    public static class Builder {
        private BeregningsresultatFeriepenger mal;

        public Builder() {
            mal = new BeregningsresultatFeriepenger();
        }

        public Builder(BeregningsresultatFeriepenger beregningsresultatFeriepenger) {
            mal = beregningsresultatFeriepenger;
        }

        public Builder leggTilBeregningsresultatFeriepengerPrÅr(BeregningsresultatFeriepengerPrÅr beregningsresultatFeriepengerPrÅrMal) {
            mal.beregningsresultatFeriepengerPrÅrListe.add(beregningsresultatFeriepengerPrÅrMal);
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

        public BeregningsresultatFeriepenger build(BeregningsresultatEntitet beregningsresultat) {
            mal.beregningsresultat = beregningsresultat;
            BeregningsresultatEntitet.builder(beregningsresultat).medBeregningsresultatFeriepenger(mal);
            verifyStateForBuild();
            return mal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(mal.beregningsresultat, "beregningsresultat");
            Objects.requireNonNull(mal.beregningsresultatFeriepengerPrÅrListe, "beregningsresultatFeriepengerPrÅrListe");
        }
    }
}
