package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.ArrayList;
import java.util.Collection;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;

public final class SykdomUtils {

    private SykdomUtils() {
    }

    public static LocalDateTimeline<SykdomVurderingVersjon> tilTidslinjeForType(Collection<SykdomVurderingVersjon> vurderinger, SykdomVurderingType type) {
        return SykdomUtils.tilTidslinje(vurderinger.stream().filter(v -> v.getSykdomVurdering().getType() == type).toList());
    }

    public static LocalDateTimeline<SykdomVurderingVersjon> tilTidslinje(Collection<SykdomVurderingVersjon> vurderinger) {
        final Collection<LocalDateSegment<SykdomVurderingVersjon>> segments = new ArrayList<>();
        for (SykdomVurderingVersjon vurdering : vurderinger) {
            for (SykdomVurderingPeriode periode : vurdering.getPerioder()) {
                segments.add(new LocalDateSegment<>(periode.getFom(), periode.getTom(), vurdering));
            }
        }

        final LocalDateTimeline<SykdomVurderingVersjon> tidslinje = new LocalDateTimeline<>(segments, (datoInterval, datoSegment, datoSegment2) -> {
            final Long rangering1 = datoSegment.getValue().getSykdomVurdering().getRangering();
            final Long rangering2 = datoSegment2.getValue().getSykdomVurdering().getRangering();
            final Long versjon1 = datoSegment.getValue().getVersjon();
            final Long versjon2 = datoSegment2.getValue().getVersjon();

            final SykdomVurderingVersjon valgtVurdering;
            if (rangering1.compareTo(rangering2) > 0) {
                valgtVurdering = datoSegment.getValue();
            } else if (rangering1.compareTo(rangering2) < 0) {
                valgtVurdering = datoSegment2.getValue();
            } else {
                valgtVurdering = (versjon1.compareTo(versjon2) > 0) ? datoSegment.getValue() : datoSegment2.getValue();
            }

            return new LocalDateSegment<>(datoInterval, valgtVurdering);
        });

        return tidslinje.compress();
    }
}
