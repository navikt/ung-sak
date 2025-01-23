package no.nav.ung.sak.ungdomsprogram.forbruktedager;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record VurderAntallDagerResultat(LocalDateTimeline<Boolean> tidslinjeNokDager, long forbrukteDager) {
}
