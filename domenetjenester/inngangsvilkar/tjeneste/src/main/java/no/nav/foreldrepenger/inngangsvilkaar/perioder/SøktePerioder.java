package no.nav.foreldrepenger.inngangsvilkaar.perioder;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.Fordeling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class SøktePerioder implements VilkårsPeriodiseringsFunksjon {

    private FordelingRepository fordelingRepository;

    SøktePerioder(FordelingRepository fordelingRepository) {
        this.fordelingRepository = fordelingRepository;
    }

    @Override
    public Set<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        final var fordeling = fordelingRepository.hentHvisEksisterer(behandlingId);


        if (fordeling.isEmpty()) {
            return Set.of();
        } else {
            final var perioder = fordeling.map(Fordeling::getPerioder).orElse(Collections.emptySet()).stream().map(FordelingPeriode::getPeriode).collect(Collectors.toSet());

            final var timeline = new LocalDateTimeline<>(perioder.stream().map(a -> new LocalDateSegment<>(a.getFomDato(), a.getTomDato(), true)).collect(Collectors.toList())).compress();

            return timeline.toSegments()
                .stream()
                .map(segment -> DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()))
                .collect(Collectors.toSet());
        }
    }
}
