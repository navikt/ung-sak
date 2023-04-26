package no.nav.k9.sak.kontrakt.opplæringspenger.godkjentopplaeringsinstitusjon;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GodkjentOpplæringsinstitusjonDto {

    @JsonProperty(value = "uuid")
    private UUID uuid;

    @JsonProperty(value = "navn")
    private String navn;

    @JsonProperty(value = "perioder")
    private List<Periode> perioder;

    public GodkjentOpplæringsinstitusjonDto() {
    }

    public GodkjentOpplæringsinstitusjonDto(UUID uuid, String navn, List<Periode> perioder) {
        this.uuid = uuid;
        this.navn = navn;
        this.perioder = perioder;
    }

    public GodkjentOpplæringsinstitusjonDto(UUID uuid, String navn, LocalDate fomDato, LocalDate tomDato) {
        this.uuid = uuid;
        this.navn = navn;
        this.perioder = List.of(new Periode(fomDato, tomDato));
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getNavn() {
        return navn;
    }

    public List<Periode> getPerioder() {
        return perioder;
    }
}
