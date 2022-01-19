package no.nav.k9.sak.domene.uttak.repo;

import java.time.LocalDate;
import java.util.Arrays;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "UTSøknadsperioder")
@Table(name = "UT_SOEKNADSPERIODER")
@Immutable
public class Søknadsperioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UT_SOEKNADSPERIODER")
    private Long id;

    @ChangeTracked
    @OneToMany(mappedBy = "søknadsperioder", cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
    private Set<Søknadsperiode> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Søknadsperioder() {
        // hibernate
    }

    public Søknadsperioder(Søknadsperiode... perioder) {
        this(Arrays.asList(perioder));
    }

    public Søknadsperioder(Collection<Søknadsperiode> perioder) {
        Objects.requireNonNull(perioder);
        this.perioder = perioder.stream()
            .peek(it -> it.setFordeling(this))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Long getId() {
        return id;
    }

    public Set<Søknadsperiode> getPerioder() {
        return perioder;
    }

    public DatoIntervallEntitet getMaksPeriode() {
        var perioder = getPerioder();
        var fom = perioder.stream()
            .map(Søknadsperiode::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .min(LocalDate::compareTo)
            .orElseThrow();
        var tom = perioder.stream()
            .map(Søknadsperiode::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo)
            .orElseThrow();

        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    @Override
    public String toString() {
        return "Søknadsperioder{" +
            "id=" + id +
            ", perioder=" + perioder +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Søknadsperioder fordeling = (Søknadsperioder) o;
        return Objects.equals(perioder, fordeling.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }
}
