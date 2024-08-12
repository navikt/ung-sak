package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.delt.UtledetUtenlandsopphold;

import java.util.*;
import java.util.stream.Collectors;

public class UtenlandsoppholdTidslinjeTjeneste {

    public static LocalDateTimeline<UtledetUtenlandsopphold> byggTidslinje(
            Map<KravDokument, List<VurdertSøktPeriode<Søknadsperiode>>> kravDokumenter,
            Set<PerioderFraSøknad> perioderFraSøknader) {
        var kravDokumenterSorted = kravDokumenter.keySet().stream().sorted(KravDokument::compareTo).collect(Collectors.toCollection(LinkedHashSet::new));
        var resultatTimeline = new LocalDateTimeline<UtledetUtenlandsopphold>(List.of());
        for (KravDokument kravDokument : kravDokumenterSorted) {
            var dokumenter = perioderFraSøknader.stream()
                .filter(it -> it.getJournalpostId().equals(kravDokument.getJournalpostId()))
                .collect(Collectors.toSet());
            if (dokumenter.size() == 1) {
                var perioderFraSøknad = dokumenter.iterator().next();
                for (UtenlandsoppholdPeriode utenlandsoppholdPeriode : perioderFraSøknad.getUtenlandsopphold()) {
                    var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(
                        utenlandsoppholdPeriode.getPeriode().getFomDato(),
                        utenlandsoppholdPeriode.getPeriode().getTomDato(),
                        new UtledetUtenlandsopphold(utenlandsoppholdPeriode.getLand(), utenlandsoppholdPeriode.getÅrsak())
                    )));
                    if (utenlandsoppholdPeriode.isAktiv()) {
                        resultatTimeline = resultatTimeline.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                    } else {
                        resultatTimeline = resultatTimeline.disjoint(timeline);
                    }
                }
            } else {
                throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + dokumenter);
            }
        }
        return resultatTimeline;
    }
}
