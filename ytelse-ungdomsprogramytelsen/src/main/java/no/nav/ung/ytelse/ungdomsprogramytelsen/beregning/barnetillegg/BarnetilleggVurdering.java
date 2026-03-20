package no.nav.ung.ytelse.ungdomsprogramytelsen.beregning.barnetillegg;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record BarnetilleggVurdering(
    LocalDateTimeline<Barnetillegg> barnetilleggTidslinje
) {
}
