package no.nav.ung.ytelse.ungdomsprogramytelsen.beregning;

import java.time.LocalDate;

public record UtledSatsInput(
    LocalDate fødselsdato,
    boolean harTriggerBeregnHøySats,
    Boolean harBeregnetHøySatsTidligere,
    LocalDate førsteDagMedYtelse
) {
}
