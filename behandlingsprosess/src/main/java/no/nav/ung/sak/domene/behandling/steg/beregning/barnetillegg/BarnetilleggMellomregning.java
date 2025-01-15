package no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record BarnetilleggMellomregning(LocalDateTimeline<Integer> antallBarnTidslinje, List<FødselOgDødInfo> barnFødselOgDødInfo) {
}
