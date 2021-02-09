package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
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
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "Søknadsperioder")
@Table(name = "PSB_SOEKNADSPERIODER")
@Immutable
public class SøknadsperioderHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PSB_SOEKNADSPERIODER")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @ChangeTracked
    @OneToMany(mappedBy = "søknadsperioder", cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
    private Set<Søknadsperiode> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    SøknadsperioderHolder() {
        // hibernate
    }

    public SøknadsperioderHolder(JournalpostId journalpostId, Søknadsperiode... perioder) {
        this(journalpostId, Arrays.asList(perioder));
    }

    public SøknadsperioderHolder(JournalpostId journalpostId, Collection<Søknadsperiode> perioder) {
        this.journalpostId = journalpostId;
        Objects.requireNonNull(perioder);
        this.perioder = new LinkedHashSet<>(perioder);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SøknadsperioderHolder that = (SøknadsperioderHolder) o;
        return Objects.equals(journalpostId, that.journalpostId)
            && Objects.equals(perioder, that.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, perioder);
    }

    @Override
    public String toString() {
        return "Søknadsperioder{" +
            "journalpostId=" + journalpostId +
            ", perioder=" + perioder +
            '}';
    }
}
