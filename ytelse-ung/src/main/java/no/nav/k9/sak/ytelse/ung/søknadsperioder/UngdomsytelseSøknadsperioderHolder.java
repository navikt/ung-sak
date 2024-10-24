package no.nav.k9.sak.ytelse.ung.søknadsperioder;

import java.util.Arrays;
import java.util.Collection;
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
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;

@Entity(name = "UngdomsytelseSøknadsperioderHolder")
@Table(name = "UNG_SOEKNADSPERIODER_HOLDER")
@Immutable
public class UngdomsytelseSøknadsperioderHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_SOEKNADSPERIODER_HOLDER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<UngdomsytelseSøknadsperioder> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseSøknadsperioderHolder() {
        // hibernate
    }

    public UngdomsytelseSøknadsperioderHolder(UngdomsytelseSøknadsperioder... perioder) {
        this(Arrays.asList(perioder));
    }

    public UngdomsytelseSøknadsperioderHolder(Collection<UngdomsytelseSøknadsperioder> perioder) {
        Objects.requireNonNull(perioder);
        this.perioder = perioder.stream()
            .map(UngdomsytelseSøknadsperioder::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Long getId() {
        return id;
    }

    public Set<UngdomsytelseSøknadsperioder> getPerioder() {
        return perioder;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsytelseSøknadsperioderHolder that = (UngdomsytelseSøknadsperioderHolder) o;
        return Objects.equals(perioder, that.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }

    @Override
    public String toString() {
        return "SøknadsperioderHolder{" +
            ", perioder=" + perioder +
            '}';
    }
}
