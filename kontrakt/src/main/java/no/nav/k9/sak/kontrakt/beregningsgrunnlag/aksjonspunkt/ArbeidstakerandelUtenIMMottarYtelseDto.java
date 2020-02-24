package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class ArbeidstakerandelUtenIMMottarYtelseDto {

    @JsonProperty(value = "andelsnr", required = true)
    @NotNull
    private long andelsnr;

    @JsonProperty(value = "mottarYtelse")
    private Boolean mottarYtelse;

    public ArbeidstakerandelUtenIMMottarYtelseDto() {
        // 
    }

    public ArbeidstakerandelUtenIMMottarYtelseDto(long andelsnr, Boolean mottarYtelse) {
        this.andelsnr = andelsnr;
        this.mottarYtelse = mottarYtelse;
    }

    public long getAndelsnr() {
        return andelsnr;
    }

    public Boolean getMottarYtelse() {
        return mottarYtelse;
    }

    public void setAndelsnr(long andelsnr) {
        this.andelsnr = andelsnr;
    }

    public void setMottarYtelse(Boolean mottarYtelse) {
        this.mottarYtelse = mottarYtelse;
    }
}
