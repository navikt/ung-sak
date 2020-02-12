package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
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

    @JsonProperty(value = "andelerLagtTilManueltIForrige")
    @Valid
    @Size(max = 200)
    private List<BeregningsgrunnlagPrStatusOgAndelDto> andelerLagtTilManueltIForrige = Collections.emptyList();

    @JsonProperty(value = "avkortetPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal avkortetPrAar;

    @JsonProperty(value = "beregnetPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal beregnetPrAar;

    @JsonProperty(value = "beregningsgrunnlagPeriodeFom")
    @NotNull
    private LocalDate beregningsgrunnlagPeriodeFom;

    @JsonProperty(value = "beregningsgrunnlagPeriodeTom")
    @NotNull
    private LocalDate beregningsgrunnlagPeriodeTom;

    @JsonProperty(value = "beregningsgrunnlagPrStatusOgAndel")
    @Valid
    @Size(max = 200)
    private List<BeregningsgrunnlagPrStatusOgAndelDto> beregningsgrunnlagPrStatusOgAndel = Collections.emptyList();

    @JsonProperty(value = "bruttoInkludertBortfaltNaturalytelsePrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal bruttoInkludertBortfaltNaturalytelsePrAar;

    @JsonProperty(value = "bruttoPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal bruttoPrAar;

    @JsonProperty(value = "dagsats")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long dagsats;

    @JsonProperty(value = "periodeAarsaker")
    @Valid
    @Size(max = 200)
    private Set<PeriodeÅrsak> periodeAarsaker = new HashSet<>();

    @JsonProperty(value = "redusertPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal redusertPrAar;

    public BeregningsgrunnlagPeriodeDto() {
        // trengs for deserialisering av JSON
    }

    public List<BeregningsgrunnlagPrStatusOgAndelDto> getAndelerLagtTilManueltIForrige() {
        return Collections.unmodifiableList(andelerLagtTilManueltIForrige);
    }

    public BigDecimal getAvkortetPrAar() {
        return avkortetPrAar;
    }

    public BigDecimal getBeregnetPrAar() {
        return beregnetPrAar;
    }

    public LocalDate getBeregningsgrunnlagPeriodeFom() {
        return beregningsgrunnlagPeriodeFom;
    }

    public LocalDate getBeregningsgrunnlagPeriodeTom() {
        return beregningsgrunnlagPeriodeTom;
    }

    public List<BeregningsgrunnlagPrStatusOgAndelDto> getBeregningsgrunnlagPrStatusOgAndel() {
        return Collections.unmodifiableList(beregningsgrunnlagPrStatusOgAndel);
    }

    public BigDecimal getBruttoInkludertBortfaltNaturalytelsePrAar() {
        return bruttoInkludertBortfaltNaturalytelsePrAar;
    }

    public BigDecimal getBruttoPrAar() {
        return bruttoPrAar;
    }

    public Long getDagsats() {
        return dagsats;
    }

    public Set<PeriodeÅrsak> getPeriodeAarsaker() {
        return Collections.unmodifiableSet(periodeAarsaker);
    }

    public BigDecimal getRedusertPrAar() {
        return redusertPrAar;
    }

    public void leggTilPeriodeAarsaker(List<PeriodeÅrsak> periodeAarsaker) {
        for (PeriodeÅrsak aarsak : periodeAarsaker) {
            leggTilPeriodeAarsak(aarsak);
        }
    }

    public void setAndeler(List<BeregningsgrunnlagPrStatusOgAndelDto> andeler) {
        this.beregningsgrunnlagPrStatusOgAndel = List.copyOf(andeler);
    }

    public void setAndelerLagtTilManueltIForrige(List<BeregningsgrunnlagPrStatusOgAndelDto> andelerLagtTilManueltIForrige) {
        this.andelerLagtTilManueltIForrige = List.copyOf(andelerLagtTilManueltIForrige);
    }

    public void setAvkortetPrAar(BigDecimal avkortetPrAar) {
        this.avkortetPrAar = avkortetPrAar;
    }

    public void setBeregnetPrAar(BigDecimal beregnetPrAar) {
        this.beregnetPrAar = beregnetPrAar;
    }

    public void setBeregningsgrunnlagPeriodeFom(LocalDate beregningsgrunnlagPeriodeFom) {
        this.beregningsgrunnlagPeriodeFom = beregningsgrunnlagPeriodeFom;
    }

    public void setBeregningsgrunnlagPeriodeTom(LocalDate beregningsgrunnlagPeriodeTom) {
        this.beregningsgrunnlagPeriodeTom = beregningsgrunnlagPeriodeTom;
    }

    public void setBeregningsgrunnlagPrStatusOgAndel(List<BeregningsgrunnlagPrStatusOgAndelDto> beregningsgrunnlagPrStatusOgAndel) {
        this.beregningsgrunnlagPrStatusOgAndel = List.copyOf(beregningsgrunnlagPrStatusOgAndel);
    }

    public void setBruttoInkludertBortfaltNaturalytelsePrAar(BigDecimal bruttoInkludertBortfaltNaturalytelsePrAar) {
        this.bruttoInkludertBortfaltNaturalytelsePrAar = bruttoInkludertBortfaltNaturalytelsePrAar;
    }

    public void setBruttoPrAar(BigDecimal bruttoPrAar) {
        this.bruttoPrAar = bruttoPrAar;
    }

    public void setDagsats(Long dagsats) {
        this.dagsats = dagsats;
    }

    public void setPeriodeAarsaker(Set<PeriodeÅrsak> periodeAarsaker) {
        this.periodeAarsaker = periodeAarsaker;
    }

    public void setRedusertPrAar(BigDecimal redusertPrAar) {
        this.redusertPrAar = redusertPrAar;
    }

    void leggTilPeriodeAarsak(PeriodeÅrsak periodeAarsak) {
        periodeAarsaker.add(periodeAarsak);
    }

}
