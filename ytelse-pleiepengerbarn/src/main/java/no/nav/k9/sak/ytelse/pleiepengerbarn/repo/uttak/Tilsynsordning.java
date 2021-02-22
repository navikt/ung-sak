package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

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
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "PsbOppgittTilsynsordning")
@Table(name = "UP_TILSYNSORDNING")
@Immutable
public class Tilsynsordning extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_TILSYNSORDNING")
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "TILSYNSORDNING_ID", nullable = false)
    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    private Set<TilsynsordningPeriode> perioder;

    @Enumerated(EnumType.STRING)
    @Column(name = "tilsynsordning_svar", nullable = false, updatable = false)
    private OppgittTilsynSvar oppgittTilsynSvar;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Tilsynsordning() {
        // hibernate
    }

    public Tilsynsordning(Collection<TilsynsordningPeriode> perioder, OppgittTilsynSvar svar) {
        this.oppgittTilsynSvar = Objects.requireNonNull(svar, "oppgittTilsynSvar");
        this.perioder = new LinkedHashSet<>(Objects.requireNonNull(perioder));
    }

    public Tilsynsordning(Tilsynsordning tilsynsordning) {
        this.oppgittTilsynSvar = tilsynsordning.getOppgittTilsynSvar();
        this.perioder = tilsynsordning.getPerioder()
            .stream()
            .map(TilsynsordningPeriode::new)
            .collect(Collectors.toSet());
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
        if (!(o instanceof Tilsynsordning))
            return false;
        Tilsynsordning fordeling = (Tilsynsordning) o;
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
