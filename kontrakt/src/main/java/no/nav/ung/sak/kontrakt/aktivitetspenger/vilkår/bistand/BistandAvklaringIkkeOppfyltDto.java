package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.validering.InputValideringRegex;
import no.nav.ung.kodeverk.vilkår.BistandsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;

/**
 * Saksbehandlers avklaring av hvorfor bistandsvilkåret ikke er oppfylt for én periode.
 * Brukes som felles undertype i {@link BistandFaktaavklaringPeriodeDto}
 */
public record BistandAvklaringIkkeOppfyltDto(
    @NotNull BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    @Size(max = 4000) @Pattern(regexp = InputValideringRegex.FRITEKST) String begrunnelse,
    boolean skalIkkeSendeVarsel,
    @Size(max = 4000) @Pattern(regexp = InputValideringRegex.FRITEKST) String fritekstTilVarsel,
    @Size(max = 4000) @Pattern(regexp = InputValideringRegex.FRITEKST) String begrunnelseIkkeVarsel,
    /** Hvor Nav har fått opplysningene fra. */
    @NotNull BistandsavklaringKildeType kilde,
    /** Påkrevd når kilde er ANNET. Skal ikke settes for andre kilder. */
    @Size(max = 1000) @Pattern(regexp = InputValideringRegex.FRITEKST) String kildeFritekst
) {

    public boolean skalSendeVarsel() {
        return !skalIkkeSendeVarsel;
    }

    @JsonIgnore
    @AssertTrue(message = "fritekstTilVarsel er påkrevd når ikkeOppfyltÅrsak krever fritekst og varsel skal sendes")
    public boolean isFritekstTilVarselGyldig() {
        if (ikkeOppfyltÅrsak == null || !skalSendeVarsel()) {
            return true; // dekkes av @NotNull på ikkeOppfyltÅrsak, evt. ikke relevant når varsel ikke sendes
        }
        return !ikkeOppfyltÅrsak.kreverFritekst() || (fritekstTilVarsel != null && !fritekstTilVarsel.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "begrunnelseIkkeVarsel skal kun settes når skalIkkeSendeVarsel er true")
    public boolean isBegrunnelseIkkeVarselGyldig() {
        return !skalSendeVarsel() || (begrunnelseIkkeVarsel == null || begrunnelseIkkeVarsel.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "kildeFritekst er påkrevd når kilde krever fritekst")
    public boolean isKildeFritekstGyldig() {
        if (kilde == null) {
            return true; // dekkes av @NotNull på kilde
        }
        return !kilde.kreverFritekst() || (kildeFritekst != null && !kildeFritekst.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "Ikke-støttet årsak for bistandsavklaring: AVKORTET")
    public boolean isIkkeOppfyltÅrsakStøttet() {
        return ikkeOppfyltÅrsak != BistandsvilkårIkkeOppfyltÅrsak.AVKORTET;
    }
}
