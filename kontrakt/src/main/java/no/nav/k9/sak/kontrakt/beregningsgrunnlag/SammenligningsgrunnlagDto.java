package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.beregningsgrunnlag.SammenligningsgrunnlagType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class SammenligningsgrunnlagDto {

    @JsonProperty(value = "sammenligningsgrunnlagFom", required = true)
    @NotNull
    private LocalDate sammenligningsgrunnlagFom;

    @JsonProperty(value = "sammenligningsgrunnlagTom", required = true)
    @NotNull
    private LocalDate sammenligningsgrunnlagTom;

    @JsonProperty(value = "rapportertPrAar", required = true)
    @NotNull
    @Digits(integer = 8, fraction = 2)
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal rapportertPrAar;

    @JsonProperty(value = "avvikPromille")
    @Min(-10000L)
    @Max(10000L)
    private Long avvikPromille;

    @JsonProperty(value = "avvikProsent")
    @Digits(integer = 4, fraction = 2)
    @DecimalMin("-1000.00")
    @DecimalMax("1000.00")
    private BigDecimal avvikProsent;

    @JsonProperty(value = "sammenligningsgrunnlagType")
    @NotNull
    @Valid
    private SammenligningsgrunnlagType sammenligningsgrunnlagType;

    @JsonProperty(value = "differanseBeregnet", required = true)
    @NotNull
    @Digits(integer = 8, fraction = 2)
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal differanseBeregnet;

    public SammenligningsgrunnlagDto() {
        // trengs for deserialisering av JSON
    }

    public LocalDate getSammenligningsgrunnlagFom() {
        return sammenligningsgrunnlagFom;
    }

    public LocalDate getSammenligningsgrunnlagTom() {
        return sammenligningsgrunnlagTom;
    }

    public BigDecimal getRapportertPrAar() {
        return rapportertPrAar;
    }

    public Long getAvvikPromille() {
        return avvikPromille;
    }

    public SammenligningsgrunnlagType getSammenligningsgrunnlagType() {
        return sammenligningsgrunnlagType;
    }

    public BigDecimal getDifferanseBeregnet() {
        return differanseBeregnet;
    }

    public void setSammenligningsgrunnlagFom(LocalDate sammenligningsgrunnlagFom) {
        this.sammenligningsgrunnlagFom = sammenligningsgrunnlagFom;
    }

    public void setSammenligningsgrunnlagTom(LocalDate sammenligningsgrunnlagTom) {
        this.sammenligningsgrunnlagTom = sammenligningsgrunnlagTom;
    }

    public void setRapportertPrAar(BigDecimal rapportertPrAar) {
        this.rapportertPrAar = rapportertPrAar;
    }

    public void setAvvikPromille(Long avvikPromille) {
        this.avvikPromille = avvikPromille;
    }

    public BigDecimal getAvvikProsent() {
        return avvikProsent;
    }

    public void setAvvikProsent(BigDecimal avvikProsent) {
        this.avvikProsent = avvikProsent;
    }

    public void setSammenligningsgrunnlagType(SammenligningsgrunnlagType sammenligningsgrunnlagType) {
        this.sammenligningsgrunnlagType = sammenligningsgrunnlagType;
    }

    public void setDifferanseBeregnet(BigDecimal differanseBeregnet) {
        this.differanseBeregnet = differanseBeregnet;
    }
}
