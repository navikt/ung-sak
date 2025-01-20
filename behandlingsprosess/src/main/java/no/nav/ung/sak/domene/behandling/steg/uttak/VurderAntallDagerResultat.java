package no.nav.ung.sak.domene.behandling.steg.uttak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record VurderAntallDagerResultat(LocalDateTimeline<Boolean> tidslinjeNokDager, long forbrukteDager) {
}
