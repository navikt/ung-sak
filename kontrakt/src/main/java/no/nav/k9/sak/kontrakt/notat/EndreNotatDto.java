package no.nav.k9.sak.kontrakt.notat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.notat.NotatGjelderType;
import no.nav.k9.sak.kontrakt.dokument.TekstValideringRegex;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record EndreNotatDto(
    @JsonProperty(value = "id")
    Long id,

    @JsonProperty(value = "notatTekst")
    @Size(max = 4000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    String notatTekst,

    @JsonProperty(value = "notatGjelderType")
    NotatGjelderType notatGjelderType,

    @JsonProperty(value = "versjon", required = true)
    @NotNull
    long versjon

) {
}
