package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.Duration;
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
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "PsbTilsynsordningPeriode")
@Table(name = "UP_TILSYNSORDNING_PERIODE")
@Immutable
public class TilsynsordningPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_TILSYNSORDNING_PERIODE")
    private Long id;

    @ChangeTracked
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
            @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Column(name = "varighet", nullable = false, updatable = false)
    private Duration varighet;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    TilsynsordningPeriode() {
    }

    public TilsynsordningPeriode(DatoIntervallEntitet periode, Duration varighet) {
        this.periode = periode;
        this.varighet = varighet;
    }

    public TilsynsordningPeriode(LocalDate fom, LocalDate tom, Duration varighet) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), varighet);
    }

    public TilsynsordningPeriode(TilsynsordningPeriode tilsynsordningPeriode) {
        this.periode = tilsynsordningPeriode.getPeriode();
        this.varighet = tilsynsordningPeriode.getVarighet();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Duration getVarighet() {
        return varighet;
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TilsynsordningPeriode))
            return false;
        TilsynsordningPeriode that = (TilsynsordningPeriode) o;
        return Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", periode=" + periode +
            ", versjon=" + versjon +
            '>';
    }

}
