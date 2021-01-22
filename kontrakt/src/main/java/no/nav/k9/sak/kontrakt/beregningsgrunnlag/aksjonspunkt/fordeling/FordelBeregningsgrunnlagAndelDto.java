package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FordelBeregningsgrunnlagAndelDto extends FordelRedigerbarAndelDto {

    @JsonProperty(value = "fastsatteVerdier", required = true)
    @Valid
    @NotNull
    private FordelFastsatteVerdierDto fastsatteVerdier;

    @JsonProperty(value = "forrigeArbeidsinntektPrÅr")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer forrigeArbeidsinntektPrÅr;

    @JsonProperty(value = "forrigeInntektskateori")
    @Valid
    private Inntektskategori forrigeInntektskategori;

    @JsonProperty(value = "forrigeRefusjonPrÅr")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer forrigeRefusjonPrÅr;

    public FordelBeregningsgrunnlagAndelDto() {
        //
    }

    public FordelFastsatteVerdierDto getFastsatteVerdier() {
        return fastsatteVerdier;
    }

    public Integer getForrigeArbeidsinntektPrÅr() {
        return forrigeArbeidsinntektPrÅr;
    }

    public Inntektskategori getForrigeInntektskategori() {
        return forrigeInntektskategori;
    }

    public Integer getForrigeRefusjonPrÅr() {
        return forrigeRefusjonPrÅr;
    }

    public void setFastsatteVerdier(FordelFastsatteVerdierDto fastsatteVerdier) {
        this.fastsatteVerdier = fastsatteVerdier;
    }

    public void setForrigeArbeidsinntektPrÅr(Integer forrigeArbeidsinntektPrÅr) {
        this.forrigeArbeidsinntektPrÅr = forrigeArbeidsinntektPrÅr;
    }

    public void setForrigeInntektskategori(Inntektskategori forrigeInntektskategori) {
        this.forrigeInntektskategori = forrigeInntektskategori;
    }

    public void setForrigeRefusjonPrÅr(Integer forrigeRefusjonPrÅr) {
        this.forrigeRefusjonPrÅr = forrigeRefusjonPrÅr;
    }
}
