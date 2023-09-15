package no.nav.k9.sak.kontrakt.notat;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.typer.Saksnummer;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record SkjulNotatDto(
    @JsonProperty(value = "uuid", required = true)
    UUID uuid,

    @JsonProperty(value = "skjul", required = true)
    boolean skjul,

    @AbacAttributt("saksnummer")
    @JsonProperty(value = SaksnummerDto.NAME, required = true)
    @NotNull
    Saksnummer saksnummer,

    @JsonProperty(value = "versjon", required = true)
    @NotNull
    long versjon

) {
}
