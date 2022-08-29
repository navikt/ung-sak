package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

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
import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "KursPeriode")
@Table(name = "UP_KURS_PERIODE")
@Immutable
public class KursPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_KURS_PERIODE")
    private Long id;

    @ChangeTracked
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "institusjon")
    private String institusjon;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    KursPeriode() {
    }

    public KursPeriode(DatoIntervallEntitet periode, String institusjon) {
        this.periode = periode;
        this.institusjon = institusjon;
    }

    public KursPeriode(LocalDate fom, LocalDate tom, String institusjon) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), institusjon);
    }

    public KursPeriode(KursPeriode it) {
        this.periode = it.periode;
        this.institusjon = it.institusjon;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getInstitusjon() {
        return institusjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KursPeriode that = (KursPeriode) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(institusjon, that.institusjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, institusjon);
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(institusjon, periode);
    }

    @Override
    public String toString() {
        return "KursPeriode{" +
            "periode=" + periode +
            ", institusjon=" + institusjon +
            '}';
    }
}
