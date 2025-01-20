package no.nav.ung.sak.behandlingslager.ytelse.sats;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record UngdomsytelseSatsResultat(
    LocalDateTimeline<UngdomsytelseSatser> resultatTidslinje,
    String regelInput,
    String regelSporing
) {
}
