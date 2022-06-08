package no.nav.k9.sak.utsatt;

import java.time.LocalDate;
import java.util.Objects;

import org.hibernate.annotations.Type;

import com.vladmihalcea.hibernate.type.range.PostgreSQLRangeType;
import com.vladmihalcea.hibernate.type.range.Range;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "UtsattPeriode")
@Table(name = "UB_PERIODE")
public class UtsattPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UB_PERIODE")
    private Long id;

    @ChangeTracked
    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UtsattPeriode() {
    }

    UtsattPeriode(UtsattPeriode it) {
        this.periode = it.periode;
    }

    public UtsattPeriode(DatoIntervallEntitet periode) {
        Objects.requireNonNull(periode);
        this.periode = periode.toRange();
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtsattPeriode trigger = (UtsattPeriode) o;
        return Objects.equals(periode, trigger.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    @Override
    public String toString() {
        return "UtsattPeriode{" +
            "periode=" + periode +
            '}';
    }
}
