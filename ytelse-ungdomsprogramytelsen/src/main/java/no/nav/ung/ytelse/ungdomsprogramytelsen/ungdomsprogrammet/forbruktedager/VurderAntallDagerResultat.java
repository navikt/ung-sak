package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record VurderAntallDagerResultat(LocalDateTimeline<Boolean> tidslinjeNokDager, int forbrukteDager) {
}
