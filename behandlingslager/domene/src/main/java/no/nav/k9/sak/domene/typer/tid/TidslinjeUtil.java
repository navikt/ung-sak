package no.nav.k9.sak.domene.typer.tid;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class TidslinjeUtil {

    public static NavigableSet<DatoIntervallEntitet> tilDatoIntervallEntiteter(LocalDateTimeline<?> timeline) {
        if (timeline.isEmpty()) {
            return Collections.emptyNavigableSet();
        } else {
            return Collections.unmodifiableNavigableSet(timeline.getLocalDateIntervals().stream().map(DatoIntervallEntitet::fra).collect(Collectors.toCollection(TreeSet::new)));
        }
    }
}
