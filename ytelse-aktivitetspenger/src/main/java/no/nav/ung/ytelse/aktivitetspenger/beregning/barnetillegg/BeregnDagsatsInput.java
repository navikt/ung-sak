package no.nav.ung.ytelse.aktivitetspenger.beregning.barnetillegg;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.time.LocalDate;
import java.util.List;

public record BeregnDagsatsInput(
    LocalDateTimeline<Boolean> perioder,
    LocalDate fødselsdato,
    boolean harTriggerBeregnHøySats,
    boolean harBeregnetHøySatsTidligere,
    List<FødselOgDødInfo> barnsFødselOgDødInformasjon) {
}
