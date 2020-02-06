package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AktivitetTomDatoMappingDto {

    @JsonProperty(value = "tom", required = true)
    private LocalDate tom;

    @JsonProperty(value = "aktiviteter")
    @NotNull
    @Valid
    @Size(max = 200)
    private List<BeregningAktivitetDto> aktiviteter;

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

    public List<BeregningAktivitetDto> getAktiviteter() {
        return aktiviteter;
    }

    public void setAktiviteter(List<BeregningAktivitetDto> aktiviteter) {
        this.aktiviteter = aktiviteter;
    }
}
