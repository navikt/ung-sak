package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import java.time.LocalDateTime;

/**
 * En bostedsavklaring slik den lagres: varslingsinnholdet ({@link BostedVarselInnhold}) sammen med
 * saksbehandlerens begrunnelser og sporingen av hvem som vurderte. Disse feltene er bevisst holdt utenfor
 * innholdet, siden de ikke vises for bruker og derfor ikke skal påvirke om et varsel må sendes på nytt.
 */
public record BostedAvklaring(
    BostedVarselInnhold innhold,
    String begrunnelse,
    String begrunnelseIkkeVarsel,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) {
}
