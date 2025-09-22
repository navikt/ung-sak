package no.nav.ung.sak.domene.behandling.steg.beregning;

import java.time.LocalDate;

public record UtledSatsInput(
    LocalDate fødselsdato,
    boolean harTriggerBeregnHøySats,
    Boolean harBeregnetHøySatsTidligere,
    LocalDate førsteDagMedYtelse
) {
}
