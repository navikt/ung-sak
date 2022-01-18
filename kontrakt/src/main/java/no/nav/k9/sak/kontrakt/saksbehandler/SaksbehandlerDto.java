package no.nav.k9.sak.kontrakt.saksbehandler;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SaksbehandlerDto {
    @Valid
    @Size
    @JsonProperty(value = "saksbehandlere")
    private Map<String, String> saksbehandlere;

    public SaksbehandlerDto() {
        saksbehandlere = new HashMap<>();
    }

    public SaksbehandlerDto(Map<String, String> saksbehandlere) {
        this.saksbehandlere = saksbehandlere;
    }

    public Map<String, String> getSaksbehandlere() {
        return saksbehandlere;
    }
}
