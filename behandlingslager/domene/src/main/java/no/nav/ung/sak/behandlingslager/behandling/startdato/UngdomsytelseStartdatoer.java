package no.nav.ung.sak.behandlingslager.behandling.startdato;

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

@Entity(name = "UngdomsytelseStartdatoer")
@Table(name = "UNG_STARTDATOER")
@Immutable
public class UngdomsytelseStartdatoer extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_STARTDATOER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "UNG_STARTDATOER_ID", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<UngdomsytelseSøktStartdato> startdatoer;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseStartdatoer() {
        // hibernate
    }

    public UngdomsytelseStartdatoer(UngdomsytelseStartdatoer periode) {
        this.startdatoer = periode.getStartdatoer()
            .stream()
            .map(UngdomsytelseSøktStartdato::new)
            .collect(Collectors.toSet());
        // hibernate
    }

    public UngdomsytelseStartdatoer(UngdomsytelseSøktStartdato... startdatoer) {
        this(Arrays.asList(startdatoer));
    }

    public UngdomsytelseStartdatoer(Collection<UngdomsytelseSøktStartdato> startdatoer) {
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
        UngdomsytelseStartdatoer that = (UngdomsytelseStartdatoer) o;
        return Objects.equals(startdatoer, that.startdatoer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startdatoer);
    }

    @Override
    public String toString() {
        return "UngdomsytelseStartdatoer{" +
            ", perioder=" + startdatoer +
            '}';
    }
}
