package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.Duration;
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

@Entity(name = "PsbUttakPeriode")
@Table(name = "UP_UTTAKSPERIODE")
@Immutable
public class UttakPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_UTTAKSPERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Column(name = "timer_per_dag", updatable = false)
    private Duration timerPleieAvBarnetPerDag;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UttakPeriode() {
    }

    public UttakPeriode(DatoIntervallEntitet periode,
                        Duration timerPleieAvBarnetPerDag) {
        this.periode = periode;
        this.timerPleieAvBarnetPerDag = timerPleieAvBarnetPerDag;
    }

    public UttakPeriode(UttakPeriode it) {
        this.periode = it.periode;
        this.timerPleieAvBarnetPerDag = it.timerPleieAvBarnetPerDag;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Duration getTimerPleieAvBarnetPerDag() {
        return timerPleieAvBarnetPerDag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UttakPeriode that = (UttakPeriode) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(timerPleieAvBarnetPerDag, that.timerPleieAvBarnetPerDag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, timerPleieAvBarnetPerDag);
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(periode);
    }
}
