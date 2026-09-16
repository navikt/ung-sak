package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;

public record VurderingAvVilkårPeriodeEtterAvklaringDto(

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    ÅpenPeriode periode,

    @JsonProperty(value = "erVilkårOppfylt", required = true)
    @NotNull
    boolean erVilkårOppfylt,

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
}
