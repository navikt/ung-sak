package no.nav.ung.ytelse.ungdomsprogramytelsen.beregning.barnetillegg;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record BarnetilleggMellomregning(LocalDateTimeline<Integer> antallBarnTidslinje, List<FødselOgDødInfo> barnFødselOgDødInfo) {
}
