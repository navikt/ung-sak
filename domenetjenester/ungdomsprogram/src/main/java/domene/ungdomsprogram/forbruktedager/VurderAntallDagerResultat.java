package domene.ungdomsprogram.forbruktedager;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record VurderAntallDagerResultat(LocalDateTimeline<Boolean> tidslinjeNokDager, long forbrukteDager) {
}
