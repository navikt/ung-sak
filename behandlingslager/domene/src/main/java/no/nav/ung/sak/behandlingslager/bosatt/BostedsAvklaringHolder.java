package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;
import org.hibernate.annotations.BatchSize;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregat/holder for bostedsavklaringer. Kan deles mellom behandlinger
 * ved revurdering uten endringer i grunnlaget.
 * Inneholder ett {@link BostedsPeriodeAvklaring} per vilkårsperiode.
 */
@Entity(name = "BostedsAvklaringHolder")
@Table(name = "BOSATT_AVKLARING_HOLDER")
public class BostedsAvklaringHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_AVKLARING_HOLDER")
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "bosatt_avklaring_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BostedsPeriodeAvklaring> periodeAvklaringer = new LinkedHashSet<>();

    public BostedsAvklaringHolder() {
    }

    BostedsAvklaringHolder(BostedsAvklaringHolder other) {
        if (other != null && other.periodeAvklaringer != null) {
            this.periodeAvklaringer = other.periodeAvklaringer.stream()
                .map(BostedsPeriodeAvklaring::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    BostedsAvklaringHolder(Set<BostedsPeriodeAvklaring> periodeAvklaringer) {
        this.periodeAvklaringer = periodeAvklaringer;
    }

    void fjernPeriodeAvklaring(Set<Periode> perioderSomSkalFjernes) {
        var fjerneTidslinje = new LocalDateTimeline<>(perioderSomSkalFjernes.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true)).collect(Collectors.toList()));
        periodeAvklaringer = hentSomTidslinje().disjoint(fjerneTidslinje)
            .toSegments().stream()
            .map(s -> s.getValue().medNyPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom())))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    void leggTilEllerErstattPeriodeAvklaringer(Collection<BostedsPeriodeAvklaring> nyePeriodeAvklaring) {
        periodeAvklaringer = byggAvklaringTidslinje(nyePeriodeAvklaring)
            .crossJoin(hentSomTidslinje())
            .toSegments().stream()
            .map(s -> s.getValue().medNyPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom())))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public LocalDateTimeline<BostedsPeriodeAvklaring> hentSomTidslinje() {
        return byggAvklaringTidslinje(periodeAvklaringer);
    }

    public Long getId() {
        return id;
    }

    public Set<BostedsPeriodeAvklaring> getPeriodeAvklaringer() {
        return Collections.unmodifiableSet(periodeAvklaringer);
    }

    public Optional<BostedsPeriodeAvklaring> getPeriodeAvklaring(UUID ref) {
        return periodeAvklaringer.stream().filter(it -> it.getReferanse().equals(ref)).findFirst();
    }

    private static LocalDateTimeline<BostedsPeriodeAvklaring> byggAvklaringTidslinje(Collection<BostedsPeriodeAvklaring> avklaringer) {
        return new LocalDateTimeline<>(
            avklaringer.stream().map(avklaring ->
                new LocalDateSegment<>(
                    avklaring.getPeriode().getFomDato(),
                    avklaring.getPeriode().getTomDato(),
                    avklaring)
            ).collect(Collectors.toList())
        );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsAvklaringHolder that)) return false;
        return Objects.equals(periodeAvklaringer, that.periodeAvklaringer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(periodeAvklaringer);
    }
}
