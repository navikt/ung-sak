package no.nav.k9.sak.kontrakt.notat;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.notat.NotatGjelderType;
import no.nav.k9.sak.kontrakt.dokument.TekstValideringRegex;

/**
 * Dto for notat entiteter. Brukes bare til serialisering.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "gjelderType")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record NotatDto(

        @JsonProperty(value = "notatId")
        UUID notatId,

        @JsonProperty(value = "notatTekst")
        @Size(max = 4000)
        @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
        @NotNull
        String notatTekst,

        @JsonProperty(value = "skjult")
        @NotNull
        boolean skjult,

        @JsonProperty("gjelderType")
        NotatGjelderType gjelderType,

        @JsonProperty(value = "kanRedigere")
        @NotNull
        boolean kanRedigere,

        @JsonProperty(value = "versjon")
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
