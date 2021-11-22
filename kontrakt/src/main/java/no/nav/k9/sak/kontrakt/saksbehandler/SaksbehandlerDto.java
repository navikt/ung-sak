package no.nav.k9.sak.kontrakt.saksbehandler;

import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class SaksbehandlerDto {
    @Valid
    @Size
    @JsonProperty(value = "saksbehandlere")
    Map<String, String> saksbehandlere;

    public SaksbehandlerDto(Map<String, String> saksbehandlere) {
        this.saksbehandlere = saksbehandlere;
    }
}
