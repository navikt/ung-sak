package no.nav.k9.sak.kontrakt.notat;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.notat.NotatGjelderType;
import no.nav.k9.sak.kontrakt.dokument.TekstValideringRegex;
//TODO valideringer

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record NotatDto(
    @JsonProperty(value = "id")
    Long id,

    @JsonProperty(value = "notatTekst", required = true)
    @Size(max = 4000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @NotNull
    String notatTekst,

    @JsonProperty(value = "fagsakId", required = true)
    @NotNull
    Long fagsakId,

    @JsonProperty(value = "notatGjelderType", required = true)
    @NotNull
    NotatGjelderType notatGjelderType,

    @JsonProperty(value = "skjult", required = true)
    @NotNull
    boolean skjult,

    @JsonProperty(value = "versjon", required = true)
    @NotNull
    long versjon,

    @JsonProperty(value = "opprettetAv")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}ÆØÅæøå\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    String opprettetAv,

    @JsonProperty(value = "opprettetTidspunkt")
    LocalDateTime opprettetTidspunkt,


    @JsonProperty(value = "endretAv")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    String endretAv,

    @JsonProperty(value = "endretTidspunkt")
    @Valid
    LocalDateTime endretTidspunkt

) {
}
