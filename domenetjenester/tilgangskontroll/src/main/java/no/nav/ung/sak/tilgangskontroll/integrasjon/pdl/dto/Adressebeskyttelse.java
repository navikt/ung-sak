package no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Adressebeskyttelse {
    private String gradering;

    @JsonCreator
    public Adressebeskyttelse(String gradering) {
        this.gradering = gradering;
    }

    public String getGradering() {
        return gradering;
    }
}
