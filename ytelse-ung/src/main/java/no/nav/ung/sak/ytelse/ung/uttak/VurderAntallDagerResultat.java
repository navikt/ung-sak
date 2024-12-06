package no.nav.ung.sak.ytelse.ung.uttak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record VurderAntallDagerResultat(LocalDateTimeline<Boolean> tidslinjeNokDager, long forbrukteDager) {
}
