package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.ferie;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.FeriePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

public class MapFerie {

    public List<LukketPeriode> map(Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravDokumenter,
                                   Set<PerioderFraSøknad> perioderFraSøknader,
                                   LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var kravDokumenterSorted = kravDokumenter.keySet().stream().sorted(KravDokument::compareTo).collect(Collectors.toCollection(LinkedHashSet::new));
        var resultatTimeline = new LocalDateTimeline<Boolean>(List.of());
        for (KravDokument kravDokument : kravDokumenterSorted) {
            var dokumenter = perioderFraSøknader.stream()
                .filter(it -> it.getJournalpostId().equals(kravDokument.getJournalpostId()))
                .collect(Collectors.toSet());
            if (dokumenter.size() == 1) {
                var søknadsperioder = kravDokumenter.get(kravDokument);
                var perioderFraSøknad = dokumenter.iterator().next();
                resultatTimeline = tilbakestillAllFerieSomOverlapperMedSøktPeriode(resultatTimeline, søknadsperioder);
                for (FeriePeriode feriePeriode : perioderFraSøknad.getFerie()) {
                    var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(feriePeriode.getPeriode().getFomDato(), feriePeriode.getPeriode().getTomDato(), true)));
                    resultatTimeline = resultatTimeline.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
            } else {
                throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + dokumenter);
            }
        }

        return resultatTimeline.compress()
            .intersection(tidslinjeTilVurdering)
            .toSegments()
            .stream()
            .filter(LocalDateSegment::getValue)
            .map(it -> new LukketPeriode(it.getFom(), it.getTom()))
            .collect(Collectors.toList());
    }

    private LocalDateTimeline<Boolean> tilbakestillAllFerieSomOverlapperMedSøktPeriode(LocalDateTimeline<Boolean> resultatTimeline, List<VurdertSøktPeriode<Søknadsperiode>> perioderFraSøknad) {
        var søknadsperioder = new LocalDateTimeline<>(perioderFraSøknad.stream().map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), false)).collect(Collectors.toList()));
        return resultatTimeline.combine(søknadsperioder, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }
}
