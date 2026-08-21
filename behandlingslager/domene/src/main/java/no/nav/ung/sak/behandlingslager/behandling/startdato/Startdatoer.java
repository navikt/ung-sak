package no.nav.ung.sak.behandlingslager.behandling.startdato;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.BatchSize;
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
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.diff.ChangeTracked;

@Entity(name = "Startdatoer")
@Table(name = "STARTDATOER")
@Immutable
public class Startdatoer extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STARTDATOER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "STARTDATOER_ID", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<SøktStartdato> startdatoer;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public Startdatoer() {
        // hibernate
    }


    public Startdatoer(Collection<SøktStartdato> startdatoer) {
        this.startdatoer = startdatoer.stream().map(SøktStartdato::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Long getId() {
        return id;
    }


    public Set<SøktStartdato> getStartdatoer() {
        return Collections.unmodifiableSet(startdatoer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Startdatoer that = (Startdatoer) o;
        return Objects.equals(startdatoer, that.startdatoer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startdatoer);
    }

    @Override
    public String toString() {
        return "Startdatoer{" +
            ", perioder=" + startdatoer +
            '}';
    }
}
