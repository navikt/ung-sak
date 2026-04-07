package no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record AktivitetspengerSatsResultat(
    LocalDateTimeline<AktivitetspengerSatsGrunnlag> resultatTidslinje,
    String regelInput,
    String regelSporing
) {
}
