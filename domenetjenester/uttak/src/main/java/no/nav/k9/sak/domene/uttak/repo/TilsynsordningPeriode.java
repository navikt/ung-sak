package no.nav.k9.sak.domene.uttak.repo;

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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "TilsynsordningPeriode")
@Table(name = "UT_TILSYNSORDNING_PERIODE")
@Immutable
public class TilsynsordningPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UT_TILSYNSORDNING_PERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
            @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ManyToOne
    @JoinColumn(name = "tilsynsordning_id", nullable = false, updatable = false, unique = true)
    private OppgittTilsynsordning tilsynsordning;

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

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setTilsynsordning(OppgittTilsynsordning tilsynsordning) {
        this.tilsynsordning = Objects.requireNonNull(tilsynsordning, "tilsynsordning");
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
