package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

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
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "PsbFeriePeriode")
@Table(name = "UP_FERIE_PERIODE")
@Immutable
public class FeriePeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_FERIE_PERIODE")
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

    FeriePeriode() {
    }

    public FeriePeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public FeriePeriode(LocalDate fom, LocalDate tom) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
    }

    public FeriePeriode(FeriePeriode feriePeriode) {
        this.periode = feriePeriode.getPeriode();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
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
