package no.nav.k9.sak.domene.uttak.repo;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "FeriePeriode")
@Table(name = "UT_FERIE_PERIODE")
@Immutable
public class FeriePeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UT_FERIE_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ManyToOne
    @JoinColumn(name = "ferie_id", nullable = false, updatable = false, unique = true)
    private Ferie ferie;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    FeriePeriode() {
    }

    public FeriePeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }
    
    public FeriePeriode(LocalDate fom, LocalDate tom) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
    
    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }

    void setFerie(Ferie ferie) {
        this.ferie = Objects.requireNonNull(ferie, "ferie");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof FeriePeriode)) return false;
        FeriePeriode that = (FeriePeriode) o;
        return Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +"<" +
            "id=" + id +
            ", periode=" + periode +
            ", versjon=" + versjon +
            '>';
    }

}
