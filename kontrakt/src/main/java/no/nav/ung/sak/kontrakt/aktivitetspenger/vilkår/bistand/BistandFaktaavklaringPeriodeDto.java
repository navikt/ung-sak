package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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

    @JsonIgnore
    @AssertTrue(message = "begrunnelseIkkeVarsel skal kun settes når skalIkkeSendeVarsel er true")
    public boolean isBegrunnelseIkkeVarselGyldig() {
        if (skalIkkeSendeVarsel || vurdering == null) {
            return true;
        }
        return vurdering.begrunnelseIkkeVarsel() == null || vurdering.begrunnelseIkkeVarsel().isBlank();
    }

}
