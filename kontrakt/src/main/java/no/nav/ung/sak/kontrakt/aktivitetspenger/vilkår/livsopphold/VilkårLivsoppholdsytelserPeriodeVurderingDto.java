package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.typer.Periode;

public record VilkårLivsoppholdsytelserPeriodeVurderingDto(

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    Periode periode,

    @JsonProperty(value = "erVilkårOppfylt", required = true)
    @NotNull
    boolean erVilkårOppfylt,

    @JsonProperty("avslagsårsak")
    @Valid
    AndreLivsoppholdsytelserIkkeOppfyltÅrsak avslagsårsak,

    @JsonProperty(value = "begrunnelse", required = true)
    @NotNull
    @Size(min = 3, max = 5000)
    @Valid
    @Pattern(regexp = Patterns.FRITEKST, message = Patterns.FRITEKST_MISMATCH_MELDING)
    String begrunnelse,

    @JsonProperty("fritekstVurderingBrev")
    @Size(max = 10000)
    @Valid
    @Pattern(regexp = Patterns.FRITEKST, message = Patterns.FRITEKST_MISMATCH_MELDING)
    String fritekstVurderingBrev
) {

    @AssertFalse(message = "Avslagsårsak må være satt dersom vilkåret ikke er oppfylt")
    public boolean isManglerAvslagsårsak() {
        return !erVilkårOppfylt && avslagsårsak == null;
    }

    @AssertFalse(message = "Avslagsårsak må ikke være satt dersom vilkåret er oppfylt")
    public boolean isHarAvslagsårsakSattVedInnvilgelse() {
        return erVilkårOppfylt && avslagsårsak != null;
    }
}
