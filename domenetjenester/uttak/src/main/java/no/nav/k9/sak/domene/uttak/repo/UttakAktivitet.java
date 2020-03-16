package no.nav.k9.sak.domene.uttak.repo;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
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

@Entity(name = "UttakAktivitet")
@Table(name = "UT_UTTAK_AKTIVITET")
public class UttakAktivitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UT_UTTAK_AKTIVITET")
    private Long id;

    @OneToMany(mappedBy = "uttak", cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
    private Set<UttakAktivitetPeriode> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UttakAktivitet() {
        // hibernate
    }

    public UttakAktivitet(Collection<UttakAktivitetPeriode> perioder) {
        Objects.requireNonNull(perioder);
        this.perioder = perioder.stream()
            .peek(it -> it.setUttak(this))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Long getId() {
        return id;
    }

    public Set<UttakAktivitetPeriode> getPerioder() {
        return perioder;
    }

    public DatoIntervallEntitet getMaksPeriode() {
        var perioder = getPerioder();
        var fom = perioder.stream()
            .map(UttakAktivitetPeriode::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .min(LocalDate::compareTo)
            .orElseThrow();
        var tom = perioder.stream()
            .map(UttakAktivitetPeriode::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo)
            .orElseThrow();

        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +"<" +
            "id=" + id +
            ", perioder=" + perioder +
            '>';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UttakAktivitet)) return false;
        var other = this.getClass().cast(o);
        return Objects.equals(perioder, other.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }
}
