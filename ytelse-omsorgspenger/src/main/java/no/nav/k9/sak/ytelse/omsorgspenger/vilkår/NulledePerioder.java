package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

class NulledePerioder implements VilkårsPeriodiseringsFunksjon {

    private OmsorgspengerGrunnlagRepository grunnlagRepository;

    NulledePerioder(OmsorgspengerGrunnlagRepository repo) {
        this.grunnlagRepository = repo;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        return utledPeriode(behandlingId, List.of());
    }

    NavigableSet<DatoIntervallEntitet> utledPeriodeFraSøknadsPerioder(OppgittFravær søknadsperioder, List<OppgittFraværPeriode> fraværPåSak) {
        var nullTimerTidslinje = opprettTidslinjeFraPerioder(søknadsperioder.getPerioder(), it -> Duration.ZERO.equals(it.getFraværPerDag()));
        var fagsakTidslinjeIkkeNull = opprettTidslinjeFraPerioder(fraværPåSak, it -> !Duration.ZERO.equals(it.getFraværPerDag()));

        nullTimerTidslinje = nullTimerTidslinje.disjoint(fagsakTidslinjeIkkeNull);

        return Collections.unmodifiableNavigableSet(nullTimerTidslinje.compress()
            .toSegments()
            .stream()
            .map(segment -> DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()))
            .collect(Collectors.toCollection(TreeSet::new)));
    }

    private LocalDateTimeline<Boolean> opprettTidslinjeFraPerioder(Collection<OppgittFraværPeriode> oppgittePerioder, Predicate<OppgittFraværPeriode> filter) {
        var timeline = new LocalDateTimeline<Boolean>(List.of());
        var perioder = oppgittePerioder
            .stream()
            .filter(filter)
            .map(OppgittFraværPeriode::getPeriode)
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toCollection(TreeSet::new));

        for (LocalDateSegment<Boolean> periode : perioder) {
            timeline = timeline.combine(new LocalDateTimeline<>(List.of(periode)), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return timeline;
    }

    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId, List<OppgittFraværPeriode> fraværPåSak) {
        var søknadsperioder = grunnlagRepository.hentOppgittFraværHvisEksisterer(behandlingId);

        if (søknadsperioder.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            return utledPeriodeFraSøknadsPerioder(søknadsperioder.get(), fraværPåSak);
        }
    }
}
