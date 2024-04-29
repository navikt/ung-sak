package no.nav.k9.sak.ytelse.opplaeringspenger.repo.godkjentopplaeringsinstitusjon;

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
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "GodkjentOpplæringsinstitusjonPeriode")
@Table(name = "GODKJENT_OPPLAERINGSINSTITUSJON_PERIODE")
public class GodkjentOpplæringsinstitusjonPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GODKJENT_OPPLAERINGSINSTITUSJON_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    GodkjentOpplæringsinstitusjonPeriode() {
    }

    public GodkjentOpplæringsinstitusjonPeriode(LocalDate fomDato, LocalDate tomDato) {
        this.periode = DatoIntervallEntitet.fra(fomDato, tomDato);
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GodkjentOpplæringsinstitusjonPeriode that = (GodkjentOpplæringsinstitusjonPeriode) o;
        return Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    @Override
    public String toString() {
        return "GodkjentOpplæringsinstitusjonPeriode{" +
            "periode=" + periode +
            '}';
    }
}
