package no.nav.k9.sak.trigger;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.BatchSize;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "Triggere")
@Table(name = "PT_TRIGGERE")
class Triggere extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PT_TRIGGERE")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "triggere_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<Trigger> triggere;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Triggere(Set<Trigger> triggere) {
        this.triggere = triggere;
    }

    public Triggere() {
    }

    public Set<Trigger> getTriggere() {
        return triggere;
    }

    @Override
    public String toString() {
        return "Triggere{}";
    }
}
