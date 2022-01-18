package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.util.Arrays;
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

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "OmsorgspengerFravær")
@Table(name = "OMP_OPPGITT_FRAVAER")
@Immutable
public class OppgittFravær extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OMP_OPPGITT_FRAVAER")
    private Long id;

    @ChangeTracked
    @OneToMany(mappedBy = "oppgittFravær", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<OppgittFraværPeriode> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    OppgittFravær() {
        // hibernate
    }

    public OppgittFravær(OppgittFraværPeriode... perioder) {
        this(Arrays.asList(perioder));
    }

    public OppgittFravær(Collection<OppgittFraværPeriode> perioder) {
        Objects.requireNonNull(perioder);
        this.perioder = perioder.stream()
            .map(OppgittFraværPeriode::new)
            .peek(it -> it.setFravær(this))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Long getId() {
        return id;
    }

    public Set<OppgittFraværPeriode> getPerioder() {
        return perioder;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", perioder=" + perioder +
            '>';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof OppgittFravær)) return false;
        var other = this.getClass().cast(o);
        return Objects.equals(perioder, other.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }
}
