package no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Person {

    private List<Adressebeskyttelse> adressebeskyttelse;

    @JsonCreator
    public Person(List<Adressebeskyttelse> adressebeskyttelse) {
        this.adressebeskyttelse = adressebeskyttelse;
    }

    public List<Adressebeskyttelse> getAdressebeskyttelse() {
        return adressebeskyttelse;
    }
}
