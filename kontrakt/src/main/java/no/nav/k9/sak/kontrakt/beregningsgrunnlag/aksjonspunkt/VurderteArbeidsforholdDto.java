package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class VurderteArbeidsforholdDto {

    @JsonProperty(value = "andelsnr", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;

    @JsonProperty(value = "opprinneligVerdi")
    private Boolean opprinneligVerdi;

    @JsonProperty(value = "tidsbegrensetArbeidsforhold", required = true)
    @NotNull
    private boolean tidsbegrensetArbeidsforhold;

    public VurderteArbeidsforholdDto(Long andelsnr,
                                     boolean tidsbegrensetArbeidsforhold,
                                     Boolean opprinneligVerdi) {
        this.andelsnr = andelsnr;
        this.tidsbegrensetArbeidsforhold = tidsbegrensetArbeidsforhold;
        this.opprinneligVerdi = opprinneligVerdi;
    }

    protected VurderteArbeidsforholdDto() {
        //
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public Boolean isOpprinneligVerdi() {
        return opprinneligVerdi;
    }

    public boolean isTidsbegrensetArbeidsforhold() {
        return tidsbegrensetArbeidsforhold;
    }
}
