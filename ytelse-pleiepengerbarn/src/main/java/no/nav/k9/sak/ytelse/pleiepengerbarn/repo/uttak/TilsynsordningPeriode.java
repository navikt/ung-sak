package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.Duration;
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
import jakarta.persistence.Version;

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
