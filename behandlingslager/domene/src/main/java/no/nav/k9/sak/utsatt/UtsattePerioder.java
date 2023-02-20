package no.nav.k9.sak.utsatt;

import java.util.Set;

import org.hibernate.annotations.BatchSize;

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
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "UtsattePerioder")
@Table(name = "UB_PERIODER")
class UtsattePerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UB_PERIODER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "perioder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<UtsattPeriode> triggere;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UtsattePerioder(Set<UtsattPeriode> triggere) {
        this.triggere = triggere;
    }

    public UtsattePerioder() {
    }

    public Set<UtsattPeriode> getTriggere() {
        return triggere;
    }

    @Override
    public String toString() {
        return "UtsattePerioder{}";
    }
}
