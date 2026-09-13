package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import java.time.LocalDateTime;

/**
 * En bistandsavklaring slik den lagres: varslingsinnholdet ({@link BistandVarselInnhold}) sammen med
 * saksbehandlerens begrunnelser og sporingen av hvem som vurderte. Disse feltene er bevisst holdt utenfor
 * innholdet, siden de ikke vises for bruker og derfor ikke skal påvirke om et varsel må sendes på nytt.
 */
public record BistandAvklaring(
    BistandVarselInnhold innhold,
    String begrunnelse,
    String begrunnelseIkkeVarsel,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) {
}
