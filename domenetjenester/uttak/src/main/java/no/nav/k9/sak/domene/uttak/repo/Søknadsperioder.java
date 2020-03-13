package no.nav.k9.sak.domene.uttak.repo;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "Søknadsperioder")
@Table(name = "UT_SOEKNADSPERIODER")
public class Søknadsperioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UT_SOEKNADSPERIODER")
    private Long id;

    @OneToMany(mappedBy = "søknadsperioder", cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
    private Set<Søknadsperiode> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Søknadsperioder() {
        // hibernate
    }

    public Søknadsperioder(Set<Søknadsperiode> perioder) {
        Objects.requireNonNull(perioder);
        this.perioder = perioder.stream()
            .peek(it -> it.setFordeling(this))
            .collect(Collectors.toSet());
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
