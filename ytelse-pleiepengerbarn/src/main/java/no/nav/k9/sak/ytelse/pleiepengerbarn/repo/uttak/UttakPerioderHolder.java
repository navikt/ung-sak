package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "PsbUttakPerioderHolder")
@Table(name = "UP_UTTAKSPERIODER_HOLDER")
@Immutable
public class UttakPerioderHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_UTTAKSPERIODER_HOLDER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<PerioderFraSøknad> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UttakPerioderHolder() {
        // hibernate
    }

    public UttakPerioderHolder(Collection<PerioderFraSøknad> perioderFraSøknad) {
        Objects.requireNonNull(perioderFraSøknad);
        this.perioder = perioderFraSøknad.stream()
            .map(PerioderFraSøknad::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Long getId() {
        return id;
    }

    public Set<PerioderFraSøknad> getPerioderFraSøknadene() {
        return perioder;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UttakPerioderHolder that = (UttakPerioderHolder) o;
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
