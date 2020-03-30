package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
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
            final var perioder = søknadsperioder.map(OppgittFravær::getPerioder).orElse(Collections.emptySet()).stream().map(OppgittFraværPeriode::getPeriode).collect(Collectors.toSet());

            final var timeline = new LocalDateTimeline<>(perioder.stream().map(a -> new LocalDateSegment<>(a.getFomDato(), a.getTomDato(), true)).collect(Collectors.toList())).compress();

            return timeline.toSegments()
                .stream()
                .map(segment -> DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()))
                .collect(Collectors.toSet());
        }
    }
}
