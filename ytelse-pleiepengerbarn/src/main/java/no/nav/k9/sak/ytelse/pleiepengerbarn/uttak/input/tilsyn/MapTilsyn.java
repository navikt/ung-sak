package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.tilsyn;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.Tilsynsordning;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

public class MapTilsyn {

    public Map<LukketPeriode, Duration> map(TreeSet<KravDokument> kravDokumenter,
                                            Set<PerioderFraSøknad> perioderFraSøknader,
                                            LocalDateTimeline<Boolean> tidslinjeTilVurdering) {

        var resultatTimeline = new LocalDateTimeline<Duration>(List.of());
        for (KravDokument kravDokument : kravDokumenter) {
            var dokumenter = perioderFraSøknader.stream()
                .filter(it -> it.getJournalpostId().equals(kravDokument.getJournalpostId()))
                .collect(Collectors.toSet());
            if (dokumenter.size() == 1) {
                var perioderFraSøknad = dokumenter.iterator().next();
                for (var periode : perioderFraSøknad.getTilsynsordning().stream().map(Tilsynsordning::getPerioder).flatMap(Collection::stream).collect(Collectors.toList())) {
                    var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), periode.getVarighet())));
                    resultatTimeline = resultatTimeline.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
            } else {
                throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + dokumenter);
            }
        }

        var result = new HashMap<LukketPeriode, Duration>();
        var segmenter = resultatTimeline.compress()
            .intersection(tidslinjeTilVurdering)
            .toSegments();
        for (LocalDateSegment<Duration> segment : segmenter) {
            result.put(new LukketPeriode(segment.getFom(), segment.getTom()), segment.getValue());
        }

        return result;
    }
}
