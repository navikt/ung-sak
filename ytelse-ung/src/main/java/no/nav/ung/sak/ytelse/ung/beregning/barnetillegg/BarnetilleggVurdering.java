package no.nav.ung.sak.ytelse.ung.beregning.barnetillegg;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record BarnetilleggVurdering(
    LocalDateTimeline<Barnetillegg> barnetilleggTidslinje,
    List<FødselOgDødInfo> relevanteBarnPersoninformasjon
) {
}
