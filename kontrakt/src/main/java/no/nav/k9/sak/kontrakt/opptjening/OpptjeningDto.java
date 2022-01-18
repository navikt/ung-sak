package no.nav.k9.sak.kontrakt.opptjening;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OpptjeningDto {

    @JsonProperty(value = "fastsattOpptjening")
    @Valid
    private FastsattOpptjeningDto fastsattOpptjening;

    @JsonProperty(value = "opptjeningAktivitetList")
    @Valid
    @Size(max = 200)
    private List<OpptjeningAktivitetDto> opptjeningAktivitetList;

    public OpptjeningDto() {
        // trengs for deserialisering av JSON
    }

    OpptjeningDto(FastsattOpptjeningDto fastsattOpptjening) {
        this.fastsattOpptjening = fastsattOpptjening;
    }

    public FastsattOpptjeningDto getFastsattOpptjening() {
        return fastsattOpptjening;
    }

    public List<OpptjeningAktivitetDto> getOpptjeningAktivitetList() {
        return opptjeningAktivitetList;
    }

    public void setFastsattOpptjening(FastsattOpptjeningDto fastsattOpptjening) {
        this.fastsattOpptjening = fastsattOpptjening;
    }

    public void setOpptjeningAktivitetList(List<OpptjeningAktivitetDto> opptjeningAktivitetList) {
        this.opptjeningAktivitetList = opptjeningAktivitetList;
    }
}
