package no.nav.k9.sak.web.app.tjenester.opplæringsinstitusjon;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GodkjentInstitusjonDto {

    @JsonProperty(value = "navn")
    private String navn;

    @JsonProperty(value = "fomDato")
    private LocalDate fomDato;

    @JsonProperty(value = "tomDato")
    private LocalDate tomDato;

    public GodkjentInstitusjonDto() {
    }

    public GodkjentInstitusjonDto(String navn, LocalDate fomDato, LocalDate tomDato) {
        this.navn = navn;
        this.fomDato = fomDato;
        this.tomDato = tomDato;
    }
}
