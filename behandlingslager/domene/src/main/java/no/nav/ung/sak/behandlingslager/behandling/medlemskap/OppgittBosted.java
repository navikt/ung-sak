package no.nav.ung.sak.behandlingslager.behandling.medlemskap;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "OppgittBosted")
@Table(name = "OPPGITT_FMEDLEMSKAP_BOSTED")
@Immutable
public class OppgittBosted extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPGITT_FMEDLEMSKAP_BOSTED")
    private Long id;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "landkode", nullable = false)
    private String landkode;

    public OppgittBosted() {
    }

    public OppgittBosted(LocalDate fom, LocalDate tom, String landkode) {
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
        Objects.requireNonNull(landkode, "landkode");
        this.periode = Range.closed(fom, tom);
        this.landkode = landkode;
    }

    OppgittBosted(OppgittBosted other) {
        this.periode = other.periode;
        this.landkode = other.landkode;
    }

    public Long getId() {
        return id;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public String getLandkode() {
        return landkode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OppgittBosted that = (OppgittBosted) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(landkode, that.landkode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, landkode);
    }

    @Override
    public String toString() {
        return "OppgittBosted{" +
            "periode=" + periode +
            ", landkode='" + landkode + '\'' +
            '}';
    }
}
