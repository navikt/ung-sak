package no.nav.k9.sak.kontrakt.dokument;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public record FritekstbrevinnholdDto(
    @Valid @NotNull @Size(max = 200) @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String overskrift,
    @Valid @NotNull @Size(max = 100000) @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String brødtekst) {

    @JsonCreator
    public FritekstbrevinnholdDto(@JsonProperty("overskrift") String overskrift, @JsonProperty("brødtekst") String brødtekst) {
        this.overskrift = overskrift;
        this.brødtekst = brødtekst;
    }
}
