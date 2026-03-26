package no.nav.ung.ytelse.ungdomsprogramytelsen.beregning.barnetillegg;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.FødselOgDødInfo;

import java.time.LocalDate;
import java.util.List;

public record BeregnDagsatsInput(
    LocalDateTimeline<Boolean> perioder,
    LocalDate fødselsdato,
    boolean harTriggerBeregnHøySats,
    boolean harBeregnetHøySatsTidligere,
    List<FødselOgDødInfo> barnsFødselOgDødInformasjon) {
}
