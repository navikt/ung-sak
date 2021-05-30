package no.nav.k9.sak.trigger;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import com.vladmihalcea.hibernate.type.range.Range;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.kodeverk.BehandlingÅrsakKodeverdiConverter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "Trigger")
@Table(name = "PT_TRIGGER")
public class Trigger extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PT_TRIGGER")
    private Long id;

    @ChangeTracked
    @Convert(converter = BehandlingÅrsakKodeverdiConverter.class)
    @Column(name = "arsak", nullable = false)
    private BehandlingÅrsakType årsak = BehandlingÅrsakType.UDEFINERT;

    @ChangeTracked
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Trigger() {
    }

    Trigger(Trigger it) {
        this.årsak = it.årsak;
        this.periode = it.periode;
    }

    public Trigger(BehandlingÅrsakType årsak, DatoIntervallEntitet periode) {
        Objects.requireNonNull(årsak);
        Objects.requireNonNull(periode);
        this.årsak = årsak;
        this.periode = periode.toRange();
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public BehandlingÅrsakType getÅrsak() {
        return årsak;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trigger trigger = (Trigger) o;
        return årsak == trigger.årsak && Objects.equals(periode, trigger.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(årsak, periode);
    }

    @Override
    public String toString() {
        return "Trigger{" +
            "årsak=" + årsak +
            ", periode=" + periode +
            '}';
    }
}
