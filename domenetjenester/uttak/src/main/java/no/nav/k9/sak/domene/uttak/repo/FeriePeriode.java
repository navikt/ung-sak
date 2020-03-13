package no.nav.k9.sak.domene.uttak.repo;

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

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "FeriePeriode")
@Table(name = "UT_FERIEPERIODE")
public class FeriePeriode extends BaseEntitet {

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

    public DatoIntervallEntitet getPeriode() {
        return periode;
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
