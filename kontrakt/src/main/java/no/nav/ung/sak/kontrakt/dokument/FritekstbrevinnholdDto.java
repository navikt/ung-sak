package no.nav.ung.sak.kontrakt.dokument;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.sak.kontrakt.Patterns;

@JsonIgnoreProperties(
    ignoreUnknown = true
)
@JsonFormat(
    shape = JsonFormat.Shape.OBJECT
)
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.ANY
)
@Deprecated
public record FritekstbrevinnholdDto(
    @Valid @NotNull @Size(max = 200) @Pattern(regexp = Patterns.FRITEKSTBREV, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String overskrift,
    @Valid @NotNull @Size(max = 100000) @Pattern(regexp = Patterns.FRITEKSTBREV, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String brødtekst) {

    @JsonCreator
    public FritekstbrevinnholdDto(@JsonProperty("overskrift") String overskrift, @JsonProperty("brødtekst") String brødtekst) {
        this.overskrift = overskrift;
        this.brødtekst = brødtekst;
    }
}
