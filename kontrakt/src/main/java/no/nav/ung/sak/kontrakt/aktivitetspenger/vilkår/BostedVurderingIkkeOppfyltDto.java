package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.validering.InputValideringRegex;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;

/**
 * Saksbehandlers vurdering av brukers bosted for én periode.
 * Brukes som felles undertype i {@link BostedFaktaavklaringPeriodeDto}
 */
public record BostedVurderingIkkeOppfyltDto(
    BostedsvilkårIkkeOppfyltÅrsak fraflyttingsÅrsak,
    @Size(max = 4000) @Pattern(regexp = InputValideringRegex.FRITEKST) String begrunnelse
) {
}
