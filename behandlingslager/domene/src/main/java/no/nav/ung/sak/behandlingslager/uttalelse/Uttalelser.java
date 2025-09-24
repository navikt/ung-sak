package no.nav.ung.sak.behandlingslager.uttalelse;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity(name = "Uttalelser")
@Table(name = "UTTALELSER")
public class Uttalelser extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.SEQUENCE, generator = "SEQ_UTTALELSER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "uttalelser_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<UttalelseV2> uttalelser;

    public Uttalelser() {
    }

    public Uttalelser(Set<UttalelseV2> uttalelser) {
        this.uttalelser = uttalelser.stream()
            .map(UttalelseV2::new)
            .collect(Collectors.toSet());
    }

    public Set<UttalelseV2> getUttalelser() {
        return uttalelser;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Uttalelser that)) return false;
        return Objects.equals(uttalelser, that.uttalelser);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uttalelser);
    }
}

