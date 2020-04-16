package no.nav.k9.sak.kontrakt.opptjening;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OpptjeningerDto {

    @Valid
    @Size(max = 100)
    @JsonProperty(value = "opptjeninger")
    private List<OpptjeningDto> opptjeninger;

    @JsonCreator
    public OpptjeningerDto(@JsonProperty(value = "opptjeninger") @Valid List<OpptjeningDto> opptjeninger) {
        this.opptjeninger = opptjeninger;
    }

    public OpptjeningerDto() {
    }

    public List<OpptjeningDto> getOpptjeninger() {
        return opptjeninger;
    }

    public void setOpptjeninger(List<OpptjeningDto> opptjeninger) {
        this.opptjeninger = opptjeninger;
    }

    @Override
    public String toString() {
        return "OpptjeningerDto{" +
            "opptjeninger=" + opptjeninger +
            '}';
    }
}
