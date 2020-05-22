package no.nav.k9.sak.ytelse.frisinn.vilkår;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPeriodiseringsFunksjon;

class SøktePerioder implements VilkårsPeriodiseringsFunksjon {

    private UttakRepository uttakRepository;

    SøktePerioder(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId);

        if (søknadsperioder.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            var perioder = søknadsperioder.map(Søknadsperioder::getPerioder).orElse(Collections.emptySet()).stream().map(Søknadsperiode::getPeriode).collect(Collectors.toSet());

            var timeline = new LocalDateTimeline<>(perioder.stream().map(a -> new LocalDateSegment<>(a.getFomDato(), a.getTomDato(), true)).collect(Collectors.toList())).compress();

            return Collections.unmodifiableNavigableSet(timeline.toSegments()
                .stream()
                .map(segment -> DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()))
                .collect(Collectors.toCollection(TreeSet::new)));
        }
    }
}
