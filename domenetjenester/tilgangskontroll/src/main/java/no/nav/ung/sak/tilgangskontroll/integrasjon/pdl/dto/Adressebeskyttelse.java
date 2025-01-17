package no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Adressebeskyttelse (String gradering) {

    public static Adressebeskyttelse fraEnum(AdressebeskyttelseGradering gradering) {
        return new Adressebeskyttelse(gradering.name());
    }
}
