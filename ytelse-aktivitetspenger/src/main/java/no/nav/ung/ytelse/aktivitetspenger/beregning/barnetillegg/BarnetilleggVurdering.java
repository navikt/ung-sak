package no.nav.ung.ytelse.aktivitetspenger.beregning.barnetillegg;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record BarnetilleggVurdering(
    LocalDateTimeline<Barnetillegg> barnetilleggTidslinje
) {
}
