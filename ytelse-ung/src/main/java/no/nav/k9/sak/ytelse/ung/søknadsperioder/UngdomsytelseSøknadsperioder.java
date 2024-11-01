package no.nav.k9.sak.ytelse.ung.søknadsperioder;

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
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "UngdomsytelseSøknadsperioder")
@Table(name = "UNG_SOEKNADSPERIODER")
@Immutable
public class UngdomsytelseSøknadsperioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_SOEKNADSPERIODER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "UNG_SOEKNADSPERIODER_ID", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<UngdomsytelseSøknadsperiode> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseSøknadsperioder() {
        // hibernate
    }

    public UngdomsytelseSøknadsperioder(UngdomsytelseSøknadsperioder periode) {
        this.perioder = periode.getPerioder()
            .stream()
            .map(UngdomsytelseSøknadsperiode::new)
            .collect(Collectors.toSet());
        // hibernate
    }

    public UngdomsytelseSøknadsperioder(UngdomsytelseSøknadsperiode... perioder) {
        this(Arrays.asList(perioder));
    }

    public UngdomsytelseSøknadsperioder(Collection<UngdomsytelseSøknadsperiode> perioder) {
        this.perioder = new LinkedHashSet<>(Objects.requireNonNull(perioder));
    }

    public Long getId() {
        return id;
    }


    public Set<UngdomsytelseSøknadsperiode> getPerioder() {
        return Collections.unmodifiableSet(perioder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsytelseSøknadsperioder that = (UngdomsytelseSøknadsperioder) o;
        return Objects.equals(perioder, that.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }

    @Override
    public String toString() {
        return "Søknadsperioder{" +
            ", perioder=" + perioder +
            '}';
    }
}
