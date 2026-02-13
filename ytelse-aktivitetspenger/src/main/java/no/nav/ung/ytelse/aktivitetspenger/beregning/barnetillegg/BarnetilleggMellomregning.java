package no.nav.ung.ytelse.aktivitetspenger.beregning.barnetillegg;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.util.List;

public record BarnetilleggMellomregning(LocalDateTimeline<Integer> antallBarnTidslinje, List<FødselOgDødInfo> barnFødselOgDødInfo) {
}
