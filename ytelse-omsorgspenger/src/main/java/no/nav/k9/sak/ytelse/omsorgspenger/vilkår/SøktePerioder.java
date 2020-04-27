package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

class SøktePerioder implements VilkårsPeriodiseringsFunksjon {

    private OmsorgspengerGrunnlagRepository uttakRepository;

    SøktePerioder(OmsorgspengerGrunnlagRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public Set<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittFraværHvisEksisterer(behandlingId);

        if (søknadsperioder.isEmpty()) {
            return Set.of();
        } else {
            return utledPeriodeFraSøknadsPerioder(søknadsperioder.get());
        }
    }

    Set<DatoIntervallEntitet> utledPeriodeFraSøknadsPerioder(OppgittFravær søknadsperioder) {
        var timeline = new LocalDateTimeline<Boolean>(List.of());
        var perioder = søknadsperioder.getPerioder()
            .stream()
            .map(OppgittFraværPeriode::getPeriode)
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toSet());

        for (LocalDateSegment<Boolean> periode : perioder) {
            timeline = timeline.combine(new LocalDateTimeline<>(List.of(periode)), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return timeline.compress()
            .toSegments()
            .stream()
            .map(segment -> DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()))
            .collect(Collectors.toSet());
    }
}
