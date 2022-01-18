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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "OppgittTilsynsordning")
@Table(name = "UT_TILSYNSORDNING")
@Immutable
public class OppgittTilsynsordning extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UT_TILSYNSORDNING")
    private Long id;

    @OneToMany(mappedBy = "tilsynsordning", cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
    private Set<TilsynsordningPeriode> perioder;

    @Enumerated(EnumType.STRING)
    @Column(name = "tilsynsordning_svar", nullable = false, updatable = false)
    private OppgittTilsynSvar oppgittTilsynSvar;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    OppgittTilsynsordning() {
        // hibernate
    }

    public OppgittTilsynsordning(Collection<TilsynsordningPeriode> perioder, OppgittTilsynSvar svar) {
        this.oppgittTilsynSvar = Objects.requireNonNull(svar, "oppgittTilsynSvar");
        this.perioder = Objects.requireNonNull(perioder)
            .stream()
            .peek(it -> it.setTilsynsordning(this))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Long getId() {
        return id;
    }

    public Set<TilsynsordningPeriode> getPerioder() {
        return perioder;
    }
    
    public OppgittTilsynSvar getOppgittTilsynSvar() {
        return oppgittTilsynSvar;
    }
    
    public boolean erTilsynsordning() {
        return OppgittTilsynSvar.JA == oppgittTilsynSvar;
    }

    public DatoIntervallEntitet getMaksPeriode() {
        var perioder = getPerioder();
        var fom = perioder.stream()
            .map(TilsynsordningPeriode::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .min(LocalDate::compareTo)
            .orElseThrow();
        var tom = perioder.stream()
            .map(TilsynsordningPeriode::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo)
            .orElseThrow();

        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
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
        if (this == o)
            return true;
        if (!(o instanceof OppgittTilsynsordning))
            return false;
        OppgittTilsynsordning fordeling = (OppgittTilsynsordning) o;
        return Objects.equals(perioder, fordeling.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }

    public enum OppgittTilsynSvar {
        JA,
        NEI,
        VET_IKKE;

    }
}
