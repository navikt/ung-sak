package no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats;

import java.time.LocalDate;

public record UtledSatsInput(
    LocalDate fødselsdato,
    boolean harTriggerBeregnHøySats,
    Boolean harBeregnetHøySatsTidligere,
    LocalDate førsteDagMedYtelse
) {
}
