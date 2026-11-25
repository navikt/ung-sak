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
    boolean skalSendeVarsel
) {

    @JsonIgnore
    @AssertTrue(message = "begrunnelseIkkeVarsel skal kun settes når skalSendeVarsel er false")
    public boolean isBegrunnelseIkkeVarselGyldig() {
        if (!skalSendeVarsel || vurdering == null) {
            return true;
        }
        return vurdering.begrunnelseIkkeVarsel() == null || vurdering.begrunnelseIkkeVarsel().isBlank();
    }

    @JsonIgnore
    @AssertTrue(message = "fritekstTilVarsel er påkrevd når fraflyttingsÅrsak krever fritekst og varsel skal sendes")
    public boolean isFritekstTilVarselGyldig() {
        if (vurdering == null || vurdering.fraflyttingsÅrsak() == null) {
            return true; // dekkes av @NotNull på vurdering/fraflyttingsÅrsak
        }
        return !skalSendeVarsel() || !vurdering.fraflyttingsÅrsak().kreverFritekst()
            || (vurdering.fritekstTilVarsel() != null && !vurdering.fritekstTilVarsel().isBlank());
    }

}
