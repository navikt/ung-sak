package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

import java.util.Arrays;
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

@Entity(name = "SøknadsperioderHolder")
@Table(name = "SP_SOEKNADSPERIODER_HOLDER")
@Immutable
public class SøknadsperioderHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SP_SOEKNADSPERIODER_HOLDER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<Søknadsperioder> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    SøknadsperioderHolder() {
        // hibernate
    }

    public SøknadsperioderHolder(Søknadsperioder... perioder) {
        this(Arrays.asList(perioder));
    }

    public SøknadsperioderHolder(Collection<Søknadsperioder> perioder) {
        Objects.requireNonNull(perioder);
        this.perioder = new LinkedHashSet<>(perioder);
    }

    public Long getId() {
        return id;
    }

    public Set<Søknadsperioder> getPerioder() {
        return perioder;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SøknadsperioderHolder that = (SøknadsperioderHolder) o;
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
