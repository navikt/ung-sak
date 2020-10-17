package no.nav.k9.sak.behandlingslager.behandling.beregning;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.typer.Beløp;

@Entity(name = "BeregningsresultatFeriepengerPrÅr")
@Table(name = "BR_FERIEPENGER_PR_AAR")
@DynamicInsert
@DynamicUpdate
public class BeregningsresultatFeriepengerPrÅr extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_FERIEPENGER_PR_AAR")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "beregningsresultat_andel_id", nullable = false, updatable = false)
    private BeregningsresultatAndel beregningsresultatAndel;

    @Column(name = "opptjeningsaar", nullable = false)
    private LocalDate opptjeningsår;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "verdi", column = @Column(name = "aarsbeloep", nullable = false)))
    @ChangeTracked
    private Beløp årsbeløp;

    public Long getId() {
        return id;
    }

    public BeregningsresultatAndel getBeregningsresultatAndel() {
        return beregningsresultatAndel;
    }

    public LocalDate getOpptjeningsår() {
        return opptjeningsår;
    }

    public Beløp getÅrsbeløp() {
        return årsbeløp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsresultatFeriepengerPrÅr)) {
            return false;
        }
        BeregningsresultatFeriepengerPrÅr other = (BeregningsresultatFeriepengerPrÅr) obj;
        return Objects.equals(this.beregningsresultatAndel, other.beregningsresultatAndel)
            && Objects.equals(this.getOpptjeningsår(), other.getOpptjeningsår())
            && Objects.equals(this.getÅrsbeløp(), other.getÅrsbeløp());
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregningsresultatAndel, opptjeningsår, årsbeløp);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<opptjeningsår=" + opptjeningsår + ", årsbeløp=" + årsbeløp + ">F";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BeregningsresultatFeriepengerPrÅr mal;

        public Builder() {
            mal = new BeregningsresultatFeriepengerPrÅr();
        }

        public Builder medOpptjeningsår(LocalDate opptjeningsår) {
            mal.opptjeningsår = opptjeningsår;
            return this;
        }

        public Builder medÅrsbeløp(Long årsbeløp) {
            mal.årsbeløp = new Beløp(BigDecimal.valueOf(årsbeløp));
            return this;
        }

        public BeregningsresultatFeriepengerPrÅr buildFor(BeregningsresultatAndel beregningsresultatAndel) {
            mal.beregningsresultatAndel = beregningsresultatAndel;
            BeregningsresultatAndel.builder(beregningsresultatAndel).leggTilBeregningsresultatFeriepengerPrÅr(mal);
            verifyStateForBuild();
            
            var m = mal;
            this.mal = null;
            return m;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(mal.beregningsresultatAndel, "beregningsresultatAndel");
            Objects.requireNonNull(mal.opptjeningsår, "opptjeningsår");
            Objects.requireNonNull(mal.årsbeløp, "årsbeløp");
        }
    }
}
