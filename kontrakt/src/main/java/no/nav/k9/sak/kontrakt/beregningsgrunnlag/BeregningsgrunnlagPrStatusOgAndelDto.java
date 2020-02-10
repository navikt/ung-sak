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
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsgrunnlagPrStatusOgAndelDto {

    @JsonProperty(value = "aktivitetStatus")
    @Valid
    private AktivitetStatus aktivitetStatus;

    @JsonProperty(value = "beregningsperiodeFom")
    private LocalDate beregningsperiodeFom;

    @JsonProperty(value = "beregningsperiodeTom")
    private LocalDate beregningsperiodeTom;

    @JsonProperty(value = "beregnetPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal beregnetPrAar;

    @JsonProperty(value = "fastsattForrigePrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal fastsattForrigePrAar;

    @JsonProperty(value = "overstyrtPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal overstyrtPrAar;

    @JsonProperty(value = "bruttoPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal bruttoPrAar;

    @JsonProperty(value = "avkortetPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal avkortetPrAar;

    @JsonProperty(value = "redusertPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal redusertPrAar;

    @JsonProperty(value = "erTidsbegrensetArbeidsforhold")
    private Boolean erTidsbegrensetArbeidsforhold;

    @JsonProperty(value = "erNyIArbeidslivet")
    private Boolean erNyIArbeidslivet;

    @JsonProperty(value = "lonnsendringIBeregningsperioden")
    private Boolean lonnsendringIBeregningsperioden;

    @JsonProperty(value = "andelsnr", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;

    @JsonProperty(value = "besteberegningPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal besteberegningPrAar;

    @JsonProperty(value = "inntektskategori", required = true)
    @NotNull
    @Valid
    private Inntektskategori inntektskategori;

    @JsonProperty(value = "arbeidsforhold")
    @Valid
    private BeregningsgrunnlagArbeidsforholdDto arbeidsforhold;

    @JsonProperty(value = "fastsattAvSaksbehandler")
    private Boolean fastsattAvSaksbehandler;

    @JsonProperty(value = "lagtTilAvSaksbehandler")
    private Boolean lagtTilAvSaksbehandler;

    @JsonProperty(value = "belopPrMndEtterAOrdningen")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal belopPrMndEtterAOrdningen;

    @JsonProperty(value = "belopPrAarEtterAOrdningen")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal belopPrAarEtterAOrdningen;

    @JsonProperty(value = "dagsats")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long dagsats;

    @JsonProperty(value = "originalDagsatsFraTilstøtendeYtelse")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long originalDagsatsFraTilstøtendeYtelse;

    @JsonProperty(value = "fordeltPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal fordeltPrAar;

    @JsonProperty(value = "erTilkommetAndel")
    private Boolean erTilkommetAndel;

    @JsonProperty(value = "skalFastsetteGrunnlag")
    private Boolean skalFastsetteGrunnlag;

    public BeregningsgrunnlagPrStatusOgAndelDto() {
        // trengs for deserialisering av JSON
    }

    @JsonGetter
    public LocalDate getBeregningsgrunnlagFom() {
        return beregningsperiodeFom;
    }

    @JsonGetter
    public LocalDate getBeregningsgrunnlagTom() {
        return beregningsperiodeTom;
    }

    public BigDecimal getBeregnetPrAar() {
        return beregnetPrAar;
    }

    public BigDecimal getOverstyrtPrAar() {
        return overstyrtPrAar;
    }

    public BigDecimal getBruttoPrAar() {
        return bruttoPrAar;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public BigDecimal getAvkortetPrAar() {
        return avkortetPrAar;
    }

    public BigDecimal getRedusertPrAar() {
        return redusertPrAar;
    }

    public Boolean getErTidsbegrensetArbeidsforhold() {
        return erTidsbegrensetArbeidsforhold;
    }

    public Boolean getErNyIArbeidslivet() {
        return erNyIArbeidslivet;
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public Boolean getLonnsendringIBeregningsperioden() {
        return lonnsendringIBeregningsperioden;
    }

    public BigDecimal getBesteberegningPrAar() {
        return besteberegningPrAar;
    }

    public BeregningsgrunnlagArbeidsforholdDto getArbeidsforhold() {
        return arbeidsforhold;
    }

    public BigDecimal getFordeltPrAar() {
        return fordeltPrAar;
    }

    public Boolean getSkalFastsetteGrunnlag() {
        return skalFastsetteGrunnlag;
    }

    public void setFordeltPrAar(BigDecimal fordeltPrAar) {
        this.fordeltPrAar = fordeltPrAar;
    }

    public void setArbeidsforhold(BeregningsgrunnlagArbeidsforholdDto arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public void setBesteberegningPrAar(BigDecimal besteberegningPrAar) {
        this.besteberegningPrAar = besteberegningPrAar;
    }

    public void setLonnsendringIBeregningsperioden(Boolean lonnsendringIBeregningsperioden) {
        this.lonnsendringIBeregningsperioden = lonnsendringIBeregningsperioden;
    }

    public void setBeregningsperiodeFom(LocalDate beregningsperiodeFom) {
        this.beregningsperiodeFom = beregningsperiodeFom;
    }

    public void setBeregningsperiodeTom(LocalDate beregningsperiodeTom) {
        this.beregningsperiodeTom = beregningsperiodeTom;
    }

    public void setBeregnetPrAar(BigDecimal beregnetPrAar) {
        this.beregnetPrAar = beregnetPrAar;
    }

    public void setOverstyrtPrAar(BigDecimal overstyrtPrAar) {
        this.overstyrtPrAar = overstyrtPrAar;
    }

    public void setBruttoPrAar(BigDecimal bruttoPrAar) {
        this.bruttoPrAar = bruttoPrAar;
    }

    public void setAktivitetStatus(AktivitetStatus aktivitetStatus) {
        this.aktivitetStatus = aktivitetStatus;
    }

    public void setAvkortetPrAar(BigDecimal avkortetPrAar) {
        this.avkortetPrAar = avkortetPrAar;
    }

    public void setRedusertPrAar(BigDecimal redusertPrAar) {
        this.redusertPrAar = redusertPrAar;
    }

    public void setErTidsbegrensetArbeidsforhold(Boolean erTidsbegrensetArbeidsforhold) {
        this.erTidsbegrensetArbeidsforhold = erTidsbegrensetArbeidsforhold;
    }

    public void setAndelsnr(Long andelsnr) {
        this.andelsnr = andelsnr;
    }

    public void setErNyIArbeidslivet(Boolean erNyIArbeidslivet) {
        this.erNyIArbeidslivet = erNyIArbeidslivet;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

    public Boolean getFastsattAvSaksbehandler() {
        return fastsattAvSaksbehandler;
    }

    public void setFastsattAvSaksbehandler(Boolean fastsattAvSaksbehandler) {
        this.fastsattAvSaksbehandler = fastsattAvSaksbehandler;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public void setLagtTilAvSaksbehandler(Boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public BigDecimal getFastsattForrigePrAar() {
        return fastsattForrigePrAar;
    }

    public void setFastsattForrigePrAar(BigDecimal fastsattForrigePrAar) {
        this.fastsattForrigePrAar = fastsattForrigePrAar;
    }

    public BigDecimal getBelopPrMndEtterAOrdningen() {
        return belopPrMndEtterAOrdningen;
    }

    public void setBelopPrMndEtterAOrdningen(BigDecimal belopPrMndEtterAOrdningen) {
        this.belopPrMndEtterAOrdningen = belopPrMndEtterAOrdningen;
    }

    public Long getDagsats() {
        return dagsats;
    }

    public void setDagsats(Long dagsats) {
        this.dagsats = dagsats;
    }

    public Long getOriginalDagsatsFraTilstøtendeYtelse() {
        return originalDagsatsFraTilstøtendeYtelse;
    }

    public void setOriginalDagsatsFraTilstøtendeYtelse(Long originalDagsatsFraTilstøtendeYtelse) {
        this.originalDagsatsFraTilstøtendeYtelse = originalDagsatsFraTilstøtendeYtelse;
    }

    public BigDecimal getBelopPrAarEtterAOrdningen() {
        return belopPrAarEtterAOrdningen;
    }

    public void setBelopPrAarEtterAOrdningen(BigDecimal belopPrAarEtterAOrdningen) {
        this.belopPrAarEtterAOrdningen = belopPrAarEtterAOrdningen;
    }

    public Boolean getErTilkommetAndel() {
        return erTilkommetAndel;
    }

    public void setErTilkommetAndel(Boolean erTilkommetAndel) {
        this.erTilkommetAndel = erTilkommetAndel;
    }

    public void setSkalFastsetteGrunnlag(Boolean skalFastsetteGrunnlag) {
        this.skalFastsetteGrunnlag = skalFastsetteGrunnlag;
    }
}
