package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.uttak;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak;

public class MapUttak {

    public List<SøktUttak> map(TreeSet<KravDokument> kravDokumenter,
                               Set<PerioderFraSøknad> perioderFraSøknader,
                               LocalDateTimeline<Boolean> tidslinjeTilVurdering) {

        var resultatTimeline = new LocalDateTimeline<WrappedUttak>(List.of());
        for (KravDokument kravDokument : kravDokumenter) {
            var dokumenter = perioderFraSøknader.stream()
                .filter(it -> it.getJournalpostId().equals(kravDokument.getJournalpostId()))
                .collect(Collectors.toSet());
            if (dokumenter.size() == 1) {
                var perioderFraSøknad = dokumenter.iterator().next();
                var uttaksPerioder = perioderFraSøknad.getUttakPerioder().stream().map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), new WrappedUttak(it))).collect(Collectors.toList());
                var timeline = new LocalDateTimeline<>(uttaksPerioder);
                resultatTimeline = resultatTimeline.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            } else {
                throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + dokumenter);
            }
        }

        return resultatTimeline.compress()
            .intersection(tidslinjeTilVurdering)
            .toSegments()
            .stream()
            .map(it -> new SøktUttak(new LukketPeriode(it.getFom(), it.getTom()), it.getValue().getPeriode().getTimerPleieAvBarnetPerDag()))
            .collect(Collectors.toList());
    }
}
