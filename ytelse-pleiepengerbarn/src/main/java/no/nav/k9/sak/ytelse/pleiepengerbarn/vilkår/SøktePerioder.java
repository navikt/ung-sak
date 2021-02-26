package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;

class SøktePerioder implements VilkårsPeriodiseringsFunksjon {

    private SøknadsperiodeRepository søknadsperiodeRepository;

    SøktePerioder(SøknadsperiodeRepository søknadsperiodeRepository) {
        this.søknadsperiodeRepository = søknadsperiodeRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId).map(SøknadsperiodeGrunnlag::getRelevantSøknadsperioder);

        if (søknadsperioder.isEmpty() || søknadsperioder.get().getPerioder().isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            var søknadsperioders = søknadsperioder.get().getPerioder();
            return utledVurderingsperioderFraSøknadsperioder(søknadsperioders);
        }
    }

    NavigableSet<DatoIntervallEntitet> utledVurderingsperioderFraSøknadsperioder(Set<Søknadsperioder> søknadsperioders) {
        var timeline = new LocalDateTimeline<Boolean>(List.of());

        for (Søknadsperioder perioder : søknadsperioders) {
            var relevantePerioder = perioder.getPerioder()
                .stream()
                .map(Søknadsperiode::getPeriode)
                .collect(Collectors.toSet());

            var relevantTimeline = new LocalDateTimeline<>(relevantePerioder.stream().map(a -> new LocalDateSegment<>(a.getFomDato(), a.getTomDato(), true)).collect(Collectors.toList())).compress();

            timeline = timeline.combine(relevantTimeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return Collections.unmodifiableNavigableSet(timeline.toSegments()
            .stream()
            .map(segment -> DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()))
            .collect(Collectors.toCollection(TreeSet::new)));
    }
}
