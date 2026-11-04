package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår;

import java.time.LocalDateTime;

/**
 * En aktivitetsavklaring slik den lagres: varslingsinnholdet ({@link AktivitetVarselInnhold}) sammen med
 * saksbehandlerens begrunnelser og sporingen av hvem som vurderte. Disse feltene er bevisst holdt utenfor
 * innholdet, siden de ikke vises for bruker og derfor ikke skal påvirke om et varsel må sendes på nytt.
 */
public record AktivitetAvklaring(
    AktivitetVarselInnhold innhold,
    String begrunnelse,
    String begrunnelseIkkeVarsel,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) {
}
