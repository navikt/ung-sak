package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;

/**
 * Saksbehandlers fakta-avklaring for hvorfor bruker ikke bor i Trondheim i en periode
 */
public record BostedFaktaavklaringPeriodeDto(
    @NotNull @Valid ÅpenPeriode periode,
    @NotNull @Valid BostedVurderingIkkeOppfyltDto vurdering,
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
