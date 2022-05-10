package no.nav.k9.sak.utsatt;

import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "UtsattBehandlingAvPeriode")
@Table(name = "UTSATT_BEHANDLING_AV")
public class UtsattBehandlingAvPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTSATT_BEHANDLING_AV")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @Immutable
    @JoinColumn(name = "perioder_id", nullable = false, updatable = false, unique = true)
    private UtsattePerioder perioder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    UtsattBehandlingAvPeriode() {
    }

    UtsattBehandlingAvPeriode(Long behandlingId, UtsattePerioder perioder) {
        this.behandlingId = behandlingId;
        this.perioder = perioder;
    }

    UtsattBehandlingAvPeriode(UtsattePerioder perioder) {
        this.perioder = perioder;
    }

    UtsattePerioder getTriggereEntity() {
        return perioder;
    }

    public Set<UtsattPeriode> getPerioder() {
        return perioder.getTriggere();
    }

    Long getId() {
        return id;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtsattBehandlingAvPeriode that = (UtsattBehandlingAvPeriode) o;
        return Objects.equals(perioder, that.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }

}
