package no.nav.ung.sak.ytelse.ung.beregning.barnetillegg;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record BarnetilleggResultat(
    LocalDateTimeline<Barnetillegg> resultat,
    List<FødselOgDødInfo> input
) {
}
