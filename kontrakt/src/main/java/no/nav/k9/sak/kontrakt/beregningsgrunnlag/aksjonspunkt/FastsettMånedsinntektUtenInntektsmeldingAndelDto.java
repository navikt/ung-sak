package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsettMånedsinntektUtenInntektsmeldingAndelDto {

    @JsonProperty(value = "andelsnr", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;

    @JsonProperty(value = "fastsattBeløp", required = true)
    @Min(0)
    @NotNull
    @Max(Integer.MAX_VALUE)
    private Integer fastsattBeløp;

    @JsonProperty(value = "inntektskategori", required = true)
    @Valid
    private Inntektskategori inntektskategori;

    public FastsettMånedsinntektUtenInntektsmeldingAndelDto() {
        // For Jackson
    }

    public FastsettMånedsinntektUtenInntektsmeldingAndelDto(Long andelsnr, FastsatteVerdierDto fastsatteVerdier) { // NOSONAR
        this.andelsnr = andelsnr;
        this.fastsattBeløp = fastsatteVerdier.getFastsattBeløp();
        this.inntektskategori = fastsatteVerdier.getInntektskategori();
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public Integer getFastsattBeløp() {
        return fastsattBeløp;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public void setAndelsnr(Long andelsnr) {
        this.andelsnr = andelsnr;
    }

    public void setFastsattBeløp(Integer fastsattBeløp) {
        this.fastsattBeløp = fastsattBeløp;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

}
