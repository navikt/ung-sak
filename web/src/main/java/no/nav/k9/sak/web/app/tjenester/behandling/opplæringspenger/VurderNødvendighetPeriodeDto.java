package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VurderNødvendighetPeriodeDto {

    @JsonProperty(value = "nødvendigOpplæring")
    private boolean nødvendigOpplæring;

    @JsonProperty(value = "fom")
    private LocalDate fom;

    @JsonProperty(value = "tom")
    private LocalDate tom;

    public VurderNødvendighetPeriodeDto() {
    }

    public VurderNødvendighetPeriodeDto(boolean nødvendigOpplæring, LocalDate fom, LocalDate tom) {
        this.nødvendigOpplæring = nødvendigOpplæring;
        this.fom = fom;
        this.tom = tom;
    }

    public boolean isNødvendigOpplæring() {
        return nødvendigOpplæring;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }
}
