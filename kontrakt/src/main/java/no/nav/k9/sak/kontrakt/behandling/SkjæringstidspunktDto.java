package no.nav.k9.sak.kontrakt.behandling;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class SkjæringstidspunktDto {

    @JsonProperty(value = "dato", required = true)
    @NotNull
    private LocalDate dato;

    public SkjæringstidspunktDto(LocalDate dato) {
        this.dato = dato;
    }

    protected SkjæringstidspunktDto() {
        // for jackson
    }

    public LocalDate getDato() {
        return dato;
    }

}
