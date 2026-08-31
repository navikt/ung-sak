package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.validering.InputValideringRegex;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;

/**
 * Saksbehandlers vurdering av brukers bosted for én periode.
 * Brukes som felles undertype i {@link BostedFaktaavklaringPeriodeDto}
 */
public record BostedVurderingIkkeOppfyltDto(
    BostedsvilkårIkkeOppfyltÅrsak fraflyttingsÅrsak,
    @Size(max = 4000) @Pattern(regexp = InputValideringRegex.FRITEKST) String begrunnelse,
    @Size(max = 4000) @Pattern(regexp = InputValideringRegex.FRITEKST) String fritekstTilVarsel,
    @Size(max = 4000) @Pattern(regexp = InputValideringRegex.FRITEKST) String begrunnelseIkkeVarsel,
    /** Hvor Nav har fått opplysningene fra. */
    @NotNull BostedsavklaringKildeType kilde,
    /** Påkrevd når kilde er ANNET. Skal ikke settes for andre kilder. */
    @Size(max = 1000) @Pattern(regexp = InputValideringRegex.FRITEKST) String kildeFritekst
) {
    @JsonIgnore
    @AssertTrue(message = "kildeFritekst er påkrevd når kilde er ANNET")
    public boolean isKildeFritekstGyldig() {
        if (kilde == BostedsavklaringKildeType.ANNET) {
            return kildeFritekst != null && !kildeFritekst.isBlank();
        }
        return true;
    }
}
