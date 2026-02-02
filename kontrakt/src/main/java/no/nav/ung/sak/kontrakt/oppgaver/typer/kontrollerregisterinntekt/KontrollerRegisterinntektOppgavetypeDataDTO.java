package no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;

import java.time.LocalDate;

/**
 * Data for oppgave om å kontrollere registerinntekt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
public record KontrollerRegisterinntektOppgavetypeDataDTO(
    @JsonProperty(value = "fraOgMed", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate fraOgMed,

    @JsonProperty(value = "tilOgMed", required = true)
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate tilOgMed,

    @JsonProperty(value = "registerinntekt", required = true)
    @NotNull
    RegisterinntektDTO registerinntekt,

    @JsonProperty(value = "gjelderDelerAvMåned", required = true)
    @NotNull
    Boolean gjelderDelerAvMåned
) implements OppgavetypeDataDTO {
}

