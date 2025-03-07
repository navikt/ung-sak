package no.nav.ung.sak.trigger;

import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "ProsessTriggere")
@Table(name = "PROSESS_TRIGGERE")
public class ProsessTriggere extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PROSESS_TRIGGERE")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @Immutable
    @JoinColumn(name = "triggere_id", nullable = false, updatable = false, unique = true)
    private Triggere triggere;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    ProsessTriggere() {
    }

    ProsessTriggere(Long behandlingId, Triggere triggere) {
        this.behandlingId = behandlingId;
        this.triggere = triggere;
    }

    ProsessTriggere(Triggere triggere) {
        this.triggere = triggere;
    }

    Triggere getTriggereEntity() {
        return triggere;
    }

    public Set<Trigger> getTriggere() {
        return triggere.getTriggere();
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
        ProsessTriggere that = (ProsessTriggere) o;
        return Objects.equals(triggere, that.triggere);
    }

    @Override
    public int hashCode() {
        return Objects.hash(triggere);
    }

}
