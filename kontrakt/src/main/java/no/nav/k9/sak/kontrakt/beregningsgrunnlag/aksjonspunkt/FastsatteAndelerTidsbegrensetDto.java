package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsatteAndelerTidsbegrensetDto {

    @JsonProperty(value = "andelsnr", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;
    
    @JsonProperty(value = "bruttoFastsattInntekt", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Integer bruttoFastsattInntekt;

    protected FastsatteAndelerTidsbegrensetDto() {
        //
    }

    public FastsatteAndelerTidsbegrensetDto(Long andelsnr,
                                            Integer bruttoFastsattInntekt) {
        this.andelsnr = andelsnr;
        this.bruttoFastsattInntekt = bruttoFastsattInntekt;
    }
    public Long getAndelsnr() { return andelsnr; }

    public Integer getBruttoFastsattInntekt() {
        return bruttoFastsattInntekt;
    }

}
