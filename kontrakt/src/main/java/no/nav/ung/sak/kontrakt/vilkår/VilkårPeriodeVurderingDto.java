package no.nav.ung.sak.kontrakt.vilkår;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.typer.Periode;

public record VilkårPeriodeVurderingDto(

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    Periode periode,

    @JsonProperty("erVilkårOppfylt")
    boolean erVilkårOppfylt,

    @JsonProperty("avslagsårsak")
    @Valid
    Avslagsårsak avslagsårsak,

    @JsonProperty(value = "begrunnelse")
    @Size(max = 5000)
    @Valid
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    String begrunnelse
) {
}
