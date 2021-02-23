package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.ferie;

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
                                   Set<PerioderFraSøknad> perioderFraSøknader) {
        var resultatTimeline = new LocalDateTimeline<Boolean>(List.of());
        for (KravDokument kravDokument : kravDokumenter) {
            var dokumenter = perioderFraSøknader.stream()
                .filter(it -> it.getJournalpostId().equals(kravDokument.getJournalpostId()))
                .collect(Collectors.toSet());
            if (dokumenter.size() == 1) {
                var perioderFraSøknad = dokumenter.iterator().next();
                for (FeriePeriode feriePeriode : perioderFraSøknad.getFerie()) {
                    var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(feriePeriode.getPeriode().getFomDato(), feriePeriode.getPeriode().getTomDato(), true)));
                    resultatTimeline = resultatTimeline.combine(timeline, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
            } else {
                throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + dokumenter);
            }
        }

        return resultatTimeline.compress()
            .toSegments()
            .stream()
            .map(it -> new LukketPeriode(it.getFom(), it.getTom()))
            .collect(Collectors.toList());
    }
}
