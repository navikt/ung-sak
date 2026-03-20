package no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;

public record AktivitetspengerSatsResultat(
    LocalDateTimeline<AktivitetspengerSatser> resultatTidslinje,
    String regelInput,
    String regelSporing
) {
}
