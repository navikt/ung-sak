package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

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
public class FastsettBeregningsgrunnlagPeriodeDto {

    @JsonProperty(value = "andeler")
    @Valid
    @Size(max = 100)
    private List<FastsettBeregningsgrunnlagAndelDto> andeler;

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "tom")
    private LocalDate tom;

    public FastsettBeregningsgrunnlagPeriodeDto() {
        //
    }

    public FastsettBeregningsgrunnlagPeriodeDto(List<FastsettBeregningsgrunnlagAndelDto> andeler, LocalDate fom, LocalDate tom) { // NOSONAR
        this.andeler = andeler;
        this.fom = fom;
        this.tom = tom;
    }

    public List<FastsettBeregningsgrunnlagAndelDto> getAndeler() {
        return andeler;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setAndeler(List<FastsettBeregningsgrunnlagAndelDto> andeler) {
        this.andeler = andeler;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }

}
