package no.nav.ung.sak.ytelse.ung.beregning;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record UngdomsytelseSatsResultat(
    LocalDateTimeline<UngdomsytelseSatser> resultatTidslinje,
    String regelInput,
    String regelSporing
) {
}
