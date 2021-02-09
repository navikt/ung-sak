package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

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

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "Søknadsperiode")
@Table(name = "PSB_SOEKNADSPERIODE")
@Immutable
public class Søknadsperiode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PSB_SOEKNADSPERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ManyToOne
    @JoinColumn(name = "soeknadsperioder_id", nullable = false, updatable = false, unique = true)
    private Søknadsperioder søknadsperioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Søknadsperiode() {
    }

    public Søknadsperiode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public Søknadsperiode(LocalDate fom, LocalDate tom) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setFordeling(Søknadsperioder fordeling) {
        this.søknadsperioder = fordeling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Søknadsperiode that = (Søknadsperiode) o;
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
