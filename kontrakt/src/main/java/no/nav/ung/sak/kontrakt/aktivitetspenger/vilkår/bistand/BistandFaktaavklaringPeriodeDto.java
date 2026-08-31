package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;

/**
 * Saksbehandlers fakta-avklaring for hvorfor bruker ikke oppfyller bistandsvilkåret i en periode.
 * Åpen tom betyr opphør fra og med fom.
 */
public record BistandFaktaavklaringPeriodeDto(
    @NotNull @Valid ÅpenPeriode periode,
    @NotNull @Valid BistandVurderingIkkeOppfyltDto vurdering,
    boolean skalIkkeSendeVarsel
) {

    public boolean skalSendeVarsel() {
        return !skalIkkeSendeVarsel;
    }

}
