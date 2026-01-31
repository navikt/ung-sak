package no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for arbeid og frilans registerinntekt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public record ArbeidOgFrilansRegisterInntektDTO(
    @JsonProperty(value = "inntekt", required = true)
    @NotNull
    Integer inntekt,

    @JsonProperty(value = "arbeidsgiver", required = true)
    @NotNull
    String arbeidsgiver,

    @JsonProperty(value = "arbeidsgiverNavn")
    String arbeidsgiverNavn
) {
}

