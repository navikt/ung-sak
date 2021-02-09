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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "Søknadsperioder")
@Table(name = "PSB_SOEKNADSPERIODER")
@Immutable
public class Søknadsperioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PSB_SOEKNADSPERIODER")
    private Long id;

    @ChangeTracked
    @OneToMany(mappedBy = "søknadsperioder", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<Søknadsperioder> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Søknadsperioder() {
        // hibernate
    }

    public Søknadsperioder(Søknadsperioder... perioder) {
        this(Arrays.asList(perioder));
    }

    public Søknadsperioder(Collection<Søknadsperioder> perioder) {
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
        Søknadsperioder that = (Søknadsperioder) o;
        return Objects.equals(perioder, that.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }
}
