package no.nav.ung.ytelse.aktivitetspenger.beregning.minsteytelse;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record AktivitetspengerMinsteytelseResultat(
    LocalDateTimeline<AktivitetspengerSatsGrunnlag> resultatTidslinje,
    String regelInput,
    String regelSporing
) {
}
