package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom;

import java.util.ArrayList;
import java.util.Collection;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;

public final class PleietrengendeTidslinjeUtils {

    private PleietrengendeTidslinjeUtils() {
    }

    public static LocalDateTimeline<PleietrengendeSykdomVurderingVersjon> tilTidslinjeForType(Collection<PleietrengendeSykdomVurderingVersjon> vurderinger, SykdomVurderingType type) {
        return PleietrengendeTidslinjeUtils.tilTidslinje(vurderinger.stream().filter(v -> v.getSykdomVurdering().getType() == type).toList());
    }

    public static LocalDateTimeline<PleietrengendeSykdomVurderingVersjon> tilTidslinje(Collection<PleietrengendeSykdomVurderingVersjon> vurderinger) {
        final Collection<LocalDateSegment<PleietrengendeSykdomVurderingVersjon>> segments = new ArrayList<>();
        for (PleietrengendeSykdomVurderingVersjon vurdering : vurderinger) {
            for (PleietrengendeSykdomVurderingVersjonPeriode periode : vurdering.getPerioder()) {
                segments.add(new LocalDateSegment<>(periode.getFom(), periode.getTom(), vurdering));
            }
        }

        final LocalDateTimeline<PleietrengendeSykdomVurderingVersjon> tidslinje = new LocalDateTimeline<>(segments, (datoInterval, datoSegment, datoSegment2) -> {
            final Long rangering1 = datoSegment.getValue().getSykdomVurdering().getRangering();
            final Long rangering2 = datoSegment2.getValue().getSykdomVurdering().getRangering();
            final Long versjon1 = datoSegment.getValue().getVersjon();
            final Long versjon2 = datoSegment2.getValue().getVersjon();

            final PleietrengendeSykdomVurderingVersjon valgtVurdering;
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
