package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsgrunnlagPeriodeDto {

    @JsonProperty(value = "beregningsgrunnlagPeriodeFom")
    @NotNull
    private LocalDate beregningsgrunnlagPeriodeFom;

    @JsonProperty(value = "beregningsgrunnlagPeriodeTom")
    @NotNull
    private LocalDate beregningsgrunnlagPeriodeTom;

    @JsonProperty(value = "beregnetPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal beregnetPrAar;

    @JsonProperty(value = "bruttoPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal bruttoPrAar;

    @JsonProperty(value = "bruttoInkludertBortfaltNaturalytelsePrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal bruttoInkludertBortfaltNaturalytelsePrAar;

    @JsonProperty(value = "avkortetPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal avkortetPrAar;

    @JsonProperty(value = "redusertPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal redusertPrAar;

    @JsonProperty(value = "periodeAarsaker")
    @Valid
    @Size(max = 200)
    private Set<PeriodeÅrsak> periodeAarsaker = new HashSet<>();

    @JsonProperty(value = "dagsats")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long dagsats;

    @JsonProperty(value = "beregningsgrunnlagPrStatusOgAndel")
    @Valid
    @Size(max = 200)
    private List<BeregningsgrunnlagPrStatusOgAndelDto> beregningsgrunnlagPrStatusOgAndel;

    @JsonProperty(value = "andelerLagtTilManueltIForrige")
    @Valid
    @Size(max = 200)
    private List<BeregningsgrunnlagPrStatusOgAndelDto> andelerLagtTilManueltIForrige;

    public BeregningsgrunnlagPeriodeDto() {
        // trengs for deserialisering av JSON
    }

    public LocalDate getBeregningsgrunnlagPeriodeFom() {
        return beregningsgrunnlagPeriodeFom;
    }

    public LocalDate getBeregningsgrunnlagPeriodeTom() {
        return beregningsgrunnlagPeriodeTom;
    }

    public BigDecimal getBeregnetPrAar() {
        return beregnetPrAar;
    }

    public BigDecimal getBruttoPrAar() {
        return bruttoPrAar;
    }

    public BigDecimal getBruttoInkludertBortfaltNaturalytelsePrAar() {
        return bruttoInkludertBortfaltNaturalytelsePrAar;
    }

    public BigDecimal getAvkortetPrAar() {
        return avkortetPrAar;
    }

    public BigDecimal getRedusertPrAar() {
        return redusertPrAar;
    }

    public Long getDagsats() {
        return dagsats;
    }

    public List<BeregningsgrunnlagPrStatusOgAndelDto> getBeregningsgrunnlagPrStatusOgAndel() {
        return beregningsgrunnlagPrStatusOgAndel;
    }

    public void setBeregningsgrunnlagPeriodeFom(LocalDate beregningsgrunnlagPeriodeFom) {
        this.beregningsgrunnlagPeriodeFom = beregningsgrunnlagPeriodeFom;
    }

    public void setBeregningsgrunnlagPeriodeTom(LocalDate beregningsgrunnlagPeriodeTom) {
        this.beregningsgrunnlagPeriodeTom = beregningsgrunnlagPeriodeTom;
    }

    public void setBeregnetPrAar(BigDecimal beregnetPrAar) {
        this.beregnetPrAar = beregnetPrAar;
    }

    public void setBruttoPrAar(BigDecimal bruttoPrAar) {
        this.bruttoPrAar = bruttoPrAar;
    }

    public void setBruttoInkludertBortfaltNaturalytelsePrAar(BigDecimal bruttoInkludertBortfaltNaturalytelsePrAar) {
        this.bruttoInkludertBortfaltNaturalytelsePrAar = bruttoInkludertBortfaltNaturalytelsePrAar;
    }

    public void setAvkortetPrAar(BigDecimal avkortetPrAar) {
        this.avkortetPrAar = avkortetPrAar;
    }

    public void setRedusertPrAar(BigDecimal redusertPrAar) {
        this.redusertPrAar = redusertPrAar;
    }

    public void setAndeler(List<BeregningsgrunnlagPrStatusOgAndelDto> andeler) {
        this.beregningsgrunnlagPrStatusOgAndel = andeler;
    }

    public void setDagsats(Long dagsats) {
        this.dagsats = dagsats;
    }

    void leggTilPeriodeAarsak(PeriodeÅrsak periodeAarsak) {
        periodeAarsaker.add(periodeAarsak);
    }

    public void leggTilPeriodeAarsaker(List<PeriodeÅrsak> periodeAarsaker) {
        for (PeriodeÅrsak aarsak : periodeAarsaker) {
            leggTilPeriodeAarsak(aarsak);
        }
    }

    public void setPeriodeAarsaker(Set<PeriodeÅrsak> periodeAarsaker) {
        this.periodeAarsaker = periodeAarsaker;
    }

    public Set<PeriodeÅrsak> getPeriodeAarsaker() {
        return periodeAarsaker;
    }

    public List<BeregningsgrunnlagPrStatusOgAndelDto> getAndelerLagtTilManueltIForrige() {
        return andelerLagtTilManueltIForrige;
    }

    public void setAndelerLagtTilManueltIForrige(List<BeregningsgrunnlagPrStatusOgAndelDto> andelerLagtTilManueltIForrige) {
        this.andelerLagtTilManueltIForrige = andelerLagtTilManueltIForrige;
    }

}
