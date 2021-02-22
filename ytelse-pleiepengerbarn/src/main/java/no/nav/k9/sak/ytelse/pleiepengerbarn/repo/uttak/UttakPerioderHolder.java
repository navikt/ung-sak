package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

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
        this.perioder = new LinkedHashSet<>(perioderFraSøknad);
    }

    public Long getId() {
        return id;
    }

    public Set<PerioderFraSøknad> getUttakPerioder() {
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
