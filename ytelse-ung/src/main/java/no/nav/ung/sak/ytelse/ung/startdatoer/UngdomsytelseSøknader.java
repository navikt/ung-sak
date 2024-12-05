package no.nav.ung.sak.ytelse.ung.startdatoer;

import java.util.Arrays;
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
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "UngdomsytelseSøknader")
@Table(name = "UNG_SOEKNADER")
@Immutable
public class UngdomsytelseSøknader extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_SOEKNADER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "UNG_SOEKNADER_ID", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<UngdomsytelseSøktStartdato> startdatoer;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseSøknader() {
        // hibernate
    }

    public UngdomsytelseSøknader(UngdomsytelseSøknader periode) {
        this.startdatoer = periode.getStartdatoer()
            .stream()
            .map(UngdomsytelseSøktStartdato::new)
            .collect(Collectors.toSet());
        // hibernate
    }

    public UngdomsytelseSøknader(UngdomsytelseSøktStartdato... startdatoer) {
        this(Arrays.asList(startdatoer));
    }

    public UngdomsytelseSøknader(Collection<UngdomsytelseSøktStartdato> startdatoer) {
        this.startdatoer = new LinkedHashSet<>(Objects.requireNonNull(startdatoer));
    }

    public Long getId() {
        return id;
    }


    public Set<UngdomsytelseSøktStartdato> getStartdatoer() {
        return Collections.unmodifiableSet(startdatoer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsytelseSøknader that = (UngdomsytelseSøknader) o;
        return Objects.equals(startdatoer, that.startdatoer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startdatoer);
    }

    @Override
    public String toString() {
        return "UngdomsytelseSøknader{" +
            ", perioder=" + startdatoer +
            '}';
    }
}
