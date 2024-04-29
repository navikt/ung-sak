package no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "VurdertOpplæringPerioderHolder")
@Table(name = "olp_vurdert_opplaering_perioder_holder")
@Immutable
public class VurdertOpplæringPerioderHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_OPPLAERING_PERIODER_HOLDER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "holder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<VurdertOpplæringPeriode> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertOpplæringPerioderHolder() {
    }

    public VurdertOpplæringPerioderHolder(List<VurdertOpplæringPeriode> perioder) {
        Objects.requireNonNull(perioder);
        this.perioder = perioder.stream()
            .map(VurdertOpplæringPeriode::new)
            .collect(Collectors.toSet());
    }

    public List<VurdertOpplæringPeriode> getPerioder() {
        return perioder.stream().toList();
    }

    public LocalDateTimeline<VurdertOpplæringPeriode> getTidslinjeOpplæring() {
        var segmenter = this.perioder.stream()
            .map(periode -> new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), periode))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segmenter);
    }
}
