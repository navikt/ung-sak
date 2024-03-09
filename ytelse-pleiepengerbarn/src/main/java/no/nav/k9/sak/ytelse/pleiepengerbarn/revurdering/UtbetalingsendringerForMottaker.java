package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record UtbetalingsendringerForMottaker(
    MottakerNøkkel nøkkel,
    LocalDateTimeline<Boolean> tidslinjeMedEndringIYtelse
) {
}
