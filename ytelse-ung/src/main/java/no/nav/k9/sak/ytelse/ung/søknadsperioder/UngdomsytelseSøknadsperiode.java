package no.nav.k9.sak.ytelse.ung.søknadsperioder;

import java.time.LocalDate;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VurdertSøktPeriode.SøktPeriodeData;

@Entity(name = "UngdomsytelseSøknadsperiode")
@Table(name = "UNG_SOEKNADSPERIODE")
@Immutable
public class UngdomsytelseSøknadsperiode extends BaseEntitet implements SøktPeriodeData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_SOEKNADSPERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;


    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseSøknadsperiode() {
    }

    public UngdomsytelseSøknadsperiode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }


    public UngdomsytelseSøknadsperiode(LocalDate fom, LocalDate tom) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
    }

    public UngdomsytelseSøknadsperiode(UngdomsytelseSøknadsperiode it) {
        this.periode = it.getPeriode();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }


    @Override
    public <V> V getPayload() {
        // skal returnere data til bruk ved komprimering av perioder (dvs. uten periode)
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsytelseSøknadsperiode that = (UngdomsytelseSøknadsperiode) o;
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
