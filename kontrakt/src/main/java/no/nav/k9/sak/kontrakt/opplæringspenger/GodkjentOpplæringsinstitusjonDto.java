package no.nav.k9.sak.kontrakt.opplæringspenger;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GodkjentOpplæringsinstitusjonDto {

    @JsonProperty(value = "uuid")
    private UUID uuid;

    @JsonProperty(value = "navn")
    private String navn;

    @JsonProperty(value = "fomDato")
    private LocalDate fomDato;

    @JsonProperty(value = "tomDato")
    private LocalDate tomDato;

    public GodkjentOpplæringsinstitusjonDto() {
    }

    public GodkjentOpplæringsinstitusjonDto(UUID uuid, String navn, LocalDate fomDato, LocalDate tomDato) {
        this.uuid = uuid;
        this.navn = navn;
        this.fomDato = fomDato;
        this.tomDato = tomDato;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }
}
