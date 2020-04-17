package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MinMaxRequest {

    @JsonProperty(value = "saksnummer", required = true)
    @Valid
    @NotNull
    private String saksnummer;

    public MinMaxRequest(@Valid @NotNull String saksnummer) {
        this.saksnummer = Objects.requireNonNull(saksnummer);
    }

    public String getSaksnummer() {
        return saksnummer;
    }
}
