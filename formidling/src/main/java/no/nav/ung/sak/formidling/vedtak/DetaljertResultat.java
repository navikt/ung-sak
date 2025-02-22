package no.nav.ung.sak.formidling.vedtak;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record DetaljertResultat(
    Set<DetaljertResultatType> resultatTyper
) {

    public static String timelineTostring(LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        return String.join(", ", detaljertResultatTidslinje.toSegments().stream()
            .map(it -> it.getLocalDateInterval().toString() +" -> "+ it.getValue().resultatTyper()).collect(Collectors.toSet()));
    }
}
