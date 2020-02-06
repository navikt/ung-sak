package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

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
public class MottarYtelseDto {

    @JsonProperty(value = "frilansMottarYtelse")
    private Boolean frilansMottarYtelse;

    @JsonProperty(value = "arbeidstakerUtenIMMottarYtelse")
    @Valid
    @Size(max = 100)
    private List<ArbeidstakerandelUtenIMMottarYtelseDto> arbeidstakerUtenIMMottarYtelse;

    protected MottarYtelseDto() {
        //
    }

    public MottarYtelseDto(Boolean frilansMottarYtelse, List<ArbeidstakerandelUtenIMMottarYtelseDto> arbeidstakerUtenIMMottarYtelse) {
        this.frilansMottarYtelse = frilansMottarYtelse;
        this.arbeidstakerUtenIMMottarYtelse = arbeidstakerUtenIMMottarYtelse;
    }

    public Boolean getFrilansMottarYtelse() {
        return frilansMottarYtelse;
    }

    public List<ArbeidstakerandelUtenIMMottarYtelseDto> getArbeidstakerUtenIMMottarYtelse() {
        return arbeidstakerUtenIMMottarYtelse;
    }

    public void setFrilansMottarYtelse(Boolean frilansMottarYtelse) {
        this.frilansMottarYtelse = frilansMottarYtelse;
    }

    public void setArbeidstakerUtenIMMottarYtelse(List<ArbeidstakerandelUtenIMMottarYtelseDto> arbeidstakerUtenIMMottarYtelse) {
        this.arbeidstakerUtenIMMottarYtelse = arbeidstakerUtenIMMottarYtelse;
    }
}
