package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.aktivitet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;

/**
 * Saksbehandlers fakta-avklaring for hvorfor bruker ikke oppfyller aktivitetsvilkåret i en periode.
 * Åpen tom betyr opphør fra og med fom.
 */
public record AktivitetFaktaavklaringPeriodeDto(
    @NotNull @Valid ÅpenPeriode periode,
    @NotNull @Valid AktivitetAvklaringIkkeOppfyltDto avklaring
) {

    public boolean skalSendeVarsel() {
        return avklaring != null && avklaring.skalSendeVarsel();
    }

}
