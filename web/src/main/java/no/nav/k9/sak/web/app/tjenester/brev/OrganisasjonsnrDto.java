package no.nav.k9.sak.web.app.tjenester.brev;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Organisasjonsnr for søk i enhetsregisteret (Brønnøysund)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record OrganisasjonsnrDto(
    @JsonProperty(value = NAME, required = true)
    @Size(max = 9)
    @NotNull
    @Pattern(regexp = "^\\d{9}$", message = "[${validatedValue}] er ugyldig. Må vere 9 siffer")
    String organisasjonsnr
) {

    public static final String NAME = "organisasjonsnr";
}
