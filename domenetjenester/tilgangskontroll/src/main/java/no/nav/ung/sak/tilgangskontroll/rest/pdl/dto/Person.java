package no.nav.ung.sak.tilgangskontroll.rest.pdl.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Person {

    private Adressebeskyttelse[] adressebeskyttelse;

    public Adressebeskyttelse[] getAdressebeskyttelse() {
        return adressebeskyttelse;
    }
    public void setAdressebeskyttelse(Adressebeskyttelse[] adressebeskyttelse) {
        this.adressebeskyttelse = adressebeskyttelse;
    }
}
