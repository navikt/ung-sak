package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.Immutable;

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

@Entity(name = "Fosterbarna")
@Table(name = "OMP_FOSTERBARNA")
@Immutable
public class Fosterbarna extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OMP_FOSTERBARNA")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ChangeTracked
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "fosterbarna_id", nullable = false)
    private Set<Fosterbarn> fosterbarn;

    Fosterbarna() {
        // Hibernate
    }

    public Fosterbarna(Set<Fosterbarn> fosterbarn) {
        Objects.requireNonNull(fosterbarn);
        if (fosterbarn.stream().anyMatch(fb -> fb.getId() != null)) {
            throw new IllegalArgumentException("Kan ikke gjenbruke allerede lagrede fosterbarn her - ta kopi");
        }
        this.fosterbarn = fosterbarn;
    }

    public Set<Fosterbarn> getFosterbarn() {
        return fosterbarn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Fosterbarna entitet = (Fosterbarna) o;
        return Objects.equals(fosterbarn, entitet.fosterbarn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fosterbarn);
    }

    @Override
    public String toString() {
        return "Fosterbarna{" +
            "id=" + id +
            ", versjon=" + versjon +
            ", fosterbarn=" + fosterbarn +
            '}';
    }
}
