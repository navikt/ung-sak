package no.nav.ung.ytelse.ungdomsprogramytelsen.beregning.barnetillegg;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.FødselOgDødInfo;

public record BarnetilleggMellomregning(LocalDateTimeline<Integer> antallBarnTidslinje, List<FødselOgDødInfo> barnFødselOgDødInfo) {
}
