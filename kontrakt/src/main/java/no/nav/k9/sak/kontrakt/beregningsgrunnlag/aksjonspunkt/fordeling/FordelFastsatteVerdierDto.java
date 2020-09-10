package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FordelFastsatteVerdierDto {

    private static final int MÅNEDER_I_1_ÅR = 12;

    @JsonProperty(value = "fastsattÅrsbeløp")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattÅrsbeløp;

    @JsonProperty(value = "fastsattBeløp")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattBeløp;

    @JsonProperty(value = "inntektskategori")
    @Valid
    private Inntektskategori inntektskategori;

    @JsonProperty(value = "refusjon")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer refusjon;

    @JsonProperty(value = "refusjonPrÅr")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer refusjonPrÅr;

    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattÅrsbeløpInklNaturalytelse;

    public FordelFastsatteVerdierDto() {
        //
    }


    public Integer getFastsattÅrsbeløp() {
        return fastsattÅrsbeløp;
    }

    public Integer getFastsattBeløp() {
        return fastsattBeløp;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Integer getRefusjon() {
        return refusjon;
    }

    public Integer getRefusjonPrÅr() {
        if (refusjonPrÅr != null) {
            return refusjonPrÅr;
        }
        return refusjon == null ? null : refusjon * MÅNEDER_I_1_ÅR;
    }


    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

    public void setRefusjon(Integer refusjon) {
        this.refusjon = refusjon;
    }

    public Integer getFastsattÅrsbeløpInklNaturalytelse() {
        return fastsattÅrsbeløpInklNaturalytelse;
    }
}
