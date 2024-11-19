package no.nav.ung.sak.kontrakt.notat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.kodeverk.notat.NotatGjelderType;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.typer.Saksnummer;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record OpprettNotatDto(

    @JsonProperty(value = "notatTekst", required = true)
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKSTBREV, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @NotNull
    String notatTekst,

    @AbacAttributt("saksnummer")
    @JsonProperty(value = SaksnummerDto.NAME, required = true)
    @NotNull
    @Valid
    Saksnummer saksnummer,

    @JsonProperty(value = "notatGjelderType", required = true)
    @NotNull
    NotatGjelderType notatGjelderType

) {
}
