package no.nav.k9.sak.kontrakt.hendelse.risikoklassifisering;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class RequestWrapper {

    @JsonProperty(value = "callId", required = true)
    @NotNull
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Alnum}\\p{P}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String callId;

    @JsonProperty(value = "request", required = true)
    @NotNull
    @Valid
    private Object request;

    public RequestWrapper(String callId, Object request) {
        this.callId = callId;
        this.request = request;
    }

    public String getCallId() {
        return callId;
    }

    public Object getRequest() {
        return request;
    }
}
