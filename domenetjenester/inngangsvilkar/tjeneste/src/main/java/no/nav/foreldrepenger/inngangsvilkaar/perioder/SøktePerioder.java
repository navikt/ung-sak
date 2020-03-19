package no.nav.foreldrepenger.inngangsvilkaar.perioder;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

class SøktePerioder implements VilkårsPeriodiseringsFunksjon {

    private UttakRepository uttakRepository;

    SøktePerioder(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public Set<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId);


        if (søknadsperioder.isEmpty()) {
            return Set.of();
        } else {
            final var perioder = søknadsperioder.map(Søknadsperioder::getPerioder).orElse(Collections.emptySet()).stream().map(Søknadsperiode::getPeriode).collect(Collectors.toSet());

            final var timeline = new LocalDateTimeline<>(perioder.stream().map(a -> new LocalDateSegment<>(a.getFomDato(), a.getTomDato(), true)).collect(Collectors.toList())).compress();

            return timeline.toSegments()
                .stream()
                .map(segment -> DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()))
                .collect(Collectors.toSet());
        }
    }
}
