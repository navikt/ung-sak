package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

class SøktePerioder implements VilkårsPeriodiseringsFunksjon {

    private OmsorgspengerGrunnlagRepository grunnlagRepository;

    SøktePerioder(OmsorgspengerGrunnlagRepository repo) {
        this.grunnlagRepository = repo;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = grunnlagRepository.hentSammenslåtteFraværPerioder(behandlingId);

        if (søknadsperioder.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            return utledPeriodeFraSøknadsPerioder(søknadsperioder);
        }
    }

    NavigableSet<DatoIntervallEntitet> utledPeriodeFraSøknadsPerioder(Collection<OppgittFraværPeriode> søktePerioder) {
        var perioder = søktePerioder
            .stream()
            .filter(it -> !Duration.ZERO.equals(it.getFraværPerDag()))
            .map(OppgittFraværPeriode::getPeriode)
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toCollection(TreeSet::new));

        var timeline = new LocalDateTimeline<Boolean>(List.of());
        for (LocalDateSegment<Boolean> periode : perioder) {
            timeline = timeline.combine(new LocalDateTimeline<>(List.of(periode)), StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return DatoIntervallEntitet.fraTimeline(timeline.compress());
    }
}
