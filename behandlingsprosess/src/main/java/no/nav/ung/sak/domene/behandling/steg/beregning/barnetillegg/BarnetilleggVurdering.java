package no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record BarnetilleggVurdering(
    LocalDateTimeline<Barnetillegg> barnetilleggTidslinje
) {
}
