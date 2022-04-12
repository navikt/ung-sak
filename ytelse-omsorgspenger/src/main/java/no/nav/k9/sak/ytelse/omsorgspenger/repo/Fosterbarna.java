package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "Fosterbarna")
@Table(name = "OMP_FOSTERBARNA")
@DynamicInsert
@DynamicUpdate
public class Fosterbarna extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OMP_FOSTERBARNA")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;


    @ChangeTracked
    @OneToMany(mappedBy = "fosterbarna", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<Fosterbarn> fosterbarn;

    Fosterbarna() {
        // Hibernate
    }

    public Fosterbarna(Set<Fosterbarn> fosterbarn) {
        Objects.requireNonNull(fosterbarn);
        this.fosterbarn = fosterbarn.stream().peek(barn -> barn.setFosterbarna(this)).collect(Collectors.toSet());
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
        return Objects.equals(versjon, entitet.versjon) && Objects.equals(fosterbarn, entitet.fosterbarn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versjon, fosterbarn);
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
