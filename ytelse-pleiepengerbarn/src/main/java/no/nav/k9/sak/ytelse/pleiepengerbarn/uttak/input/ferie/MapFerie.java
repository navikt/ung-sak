package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.ferie;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.FeriePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

public class MapFerie {

    public List<LukketPeriode> map(Set<KravDokument> kravDokumenter,
                                   Set<PerioderFraSøknad> perioderFraSøknader,
                                   LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var kravDokumenterSorted = kravDokumenter.stream().sorted(KravDokument::compareTo).collect(Collectors.toCollection(LinkedHashSet::new));
        var resultatTimeline = new LocalDateTimeline<Boolean>(List.of());
        for (KravDokument kravDokument : kravDokumenterSorted) {
            var dokumenter = perioderFraSøknader.stream()
                .filter(it -> it.getJournalpostId().equals(kravDokument.getJournalpostId()))
                .collect(Collectors.toSet());
            if (dokumenter.size() == 1) {
                var perioderFraSøknad = dokumenter.iterator().next();
                resultatTimeline = tilbakestillAllFerieSomOverlapperMedSøktPeriode(resultatTimeline, perioderFraSøknad);
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

    private LocalDateTimeline<Boolean> tilbakestillAllFerieSomOverlapperMedSøktPeriode(LocalDateTimeline<Boolean> resultatTimeline, PerioderFraSøknad perioderFraSøknad) {
        return resultatTimeline.combine(new LocalDateSegment<>(perioderFraSøknad.utledSøktPeriode().getFomDato(), perioderFraSøknad.utledSøktPeriode().getTomDato(), false), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }
}
