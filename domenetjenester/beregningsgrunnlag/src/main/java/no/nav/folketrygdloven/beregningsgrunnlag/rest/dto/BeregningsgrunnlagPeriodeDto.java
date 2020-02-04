package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsgrunnlagPeriodeDto {

    @JsonProperty("beregningsgrunnlagPeriodeFom")
    private LocalDate beregningsgrunnlagPeriodeFom;

    @JsonProperty("beregningsgrunnlagPeriodeTom")
    private LocalDate beregningsgrunnlagPeriodeTom;

    @JsonProperty("beregnetPrAar")
    private BigDecimal beregnetPrAar;

    @JsonProperty("bruttoPrAar")
    private BigDecimal bruttoPrAar;

    @JsonProperty("bruttoInkludertBortfaltNaturalytelsePrAar")
    private BigDecimal bruttoInkludertBortfaltNaturalytelsePrAar;

    @JsonProperty("avkortetPrAar")
    private BigDecimal avkortetPrAar;

    @JsonProperty("redusertPrAar")
    private BigDecimal redusertPrAar;

    @JsonProperty("periodeAarsaker")
    private Set<PeriodeÅrsak> periodeAarsaker = new HashSet<>();

    @JsonProperty("dagsats")
    private Long dagsats;

    @JsonProperty("beregningsgrunnlagPrStatusOgAndel")
    private List<BeregningsgrunnlagPrStatusOgAndelDto> beregningsgrunnlagPrStatusOgAndel;

    @JsonProperty("andelerLagtTilManueltIForrige")
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
