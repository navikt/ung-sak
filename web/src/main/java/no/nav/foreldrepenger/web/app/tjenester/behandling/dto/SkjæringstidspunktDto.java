package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SkjæringstidspunktDto {

    @JsonProperty("dato")
    private LocalDate dato;

    protected SkjæringstidspunktDto() {
        // for jackson
    }
    
    public SkjæringstidspunktDto(LocalDate dato) {
        this.dato = dato;
    }
    
    public LocalDate getDato() {
        return dato;
    }

}
