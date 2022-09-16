package no.nav.k9.sak.kontrakt.opplæringspenger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.dokument.TekstValideringRegex;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VurderInstitusjonDto {

    @JsonProperty(value = "institusjon")
    @Valid
    @Size(max = 100)
    @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String institusjon;

    @JsonProperty(value = "godkjent")
    private boolean godkjent;

    public VurderInstitusjonDto() {
    }

    public VurderInstitusjonDto(String institusjon, boolean godkjent) {
        this.institusjon = institusjon;
        this.godkjent = godkjent;
    }

    public String getInstitusjon() {
        return institusjon;
    }

    public boolean isGodkjent() {
        return godkjent;
    }
}
