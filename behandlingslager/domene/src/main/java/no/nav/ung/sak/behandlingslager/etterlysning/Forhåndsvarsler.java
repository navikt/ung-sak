package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import java.util.Objects;
import java.util.Set;


@Entity(name = "Forhåndsvarsler")
@Table(name = "FORH_VARSLER")
@Immutable
public class Forhåndsvarsler extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.SEQUENCE, generator = "SEQ_FORH_VARSLER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "forhåndsvarsler_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<Etterlysning> varsler;

    public Forhåndsvarsler() {
    }

    public Forhåndsvarsler(Set<Etterlysning> varsler) {
        this.varsler = varsler;
    }

    public Set<Etterlysning> getVarsler() {
        return varsler;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Forhåndsvarsler that)) return false;
        return Objects.equals(varsler, that.varsler);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(varsler);
    }
}
