package no.nav.ung.sak.ytelse.uttak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record VurderAntallDagerResultat(LocalDateTimeline<Boolean> tidslinjeNokDager, long forbrukteDager) {
}
