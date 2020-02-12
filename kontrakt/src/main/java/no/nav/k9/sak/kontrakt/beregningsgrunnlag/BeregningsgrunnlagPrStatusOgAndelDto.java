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

    @JsonProperty(value = "andelsnr", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;

    @JsonProperty(value = "arbeidsforhold")
    @Valid
    private BeregningsgrunnlagArbeidsforholdDto arbeidsforhold;

    @JsonProperty(value = "avkortetPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal avkortetPrAar;

    @JsonProperty(value = "belopPrAarEtterAOrdningen")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal belopPrAarEtterAOrdningen;

    @JsonProperty(value = "belopPrMndEtterAOrdningen")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal belopPrMndEtterAOrdningen;

    @JsonProperty(value = "beregnetPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal beregnetPrAar;

    @JsonProperty(value = "beregningsperiodeFom")
    private LocalDate beregningsperiodeFom;

    @JsonProperty(value = "beregningsperiodeTom")
    private LocalDate beregningsperiodeTom;

    @JsonProperty(value = "besteberegningPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal besteberegningPrAar;

    @JsonProperty(value = "bruttoPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal bruttoPrAar;

    @JsonProperty(value = "dagsats")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long dagsats;

    @JsonProperty(value = "erNyIArbeidslivet")
    private Boolean erNyIArbeidslivet;

    @JsonProperty(value = "erTidsbegrensetArbeidsforhold")
    private Boolean erTidsbegrensetArbeidsforhold;

    @JsonProperty(value = "erTilkommetAndel")
    private Boolean erTilkommetAndel;

    @JsonProperty(value = "fastsattAvSaksbehandler")
    private Boolean fastsattAvSaksbehandler;

    @JsonProperty(value = "fastsattForrigePrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal fastsattForrigePrAar;

    @JsonProperty(value = "fordeltPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal fordeltPrAar;

    @JsonProperty(value = "inntektskategori", required = true)
    @NotNull
    @Valid
    private Inntektskategori inntektskategori;

    @JsonProperty(value = "lagtTilAvSaksbehandler")
    private Boolean lagtTilAvSaksbehandler;

    @JsonProperty(value = "lonnsendringIBeregningsperioden")
    private Boolean lonnsendringIBeregningsperioden;

    @JsonProperty(value = "originalDagsatsFraTilstøtendeYtelse")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long originalDagsatsFraTilstøtendeYtelse;

    @JsonProperty(value = "overstyrtPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal overstyrtPrAar;

    @JsonProperty(value = "redusertPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal redusertPrAar;

    @JsonProperty(value = "skalFastsetteGrunnlag")
    private Boolean skalFastsetteGrunnlag;

    public BeregningsgrunnlagPrStatusOgAndelDto() {
        // trengs for deserialisering av JSON
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public BeregningsgrunnlagArbeidsforholdDto getArbeidsforhold() {
        return arbeidsforhold;
    }

    public BigDecimal getAvkortetPrAar() {
        return avkortetPrAar;
    }

    public BigDecimal getBelopPrAarEtterAOrdningen() {
        return belopPrAarEtterAOrdningen;
    }

    public BigDecimal getBelopPrMndEtterAOrdningen() {
        return belopPrMndEtterAOrdningen;
    }

    public BigDecimal getBeregnetPrAar() {
        return beregnetPrAar;
    }

    @JsonGetter
    public LocalDate getBeregningsgrunnlagFom() {
        return beregningsperiodeFom;
    }

    @JsonGetter
    public LocalDate getBeregningsgrunnlagTom() {
        return beregningsperiodeTom;
    }

    public LocalDate getBeregningsperiodeFom() {
        return beregningsperiodeFom;
    }

    public LocalDate getBeregningsperiodeTom() {
        return beregningsperiodeTom;
    }

    public BigDecimal getBesteberegningPrAar() {
        return besteberegningPrAar;
    }

    public BigDecimal getBruttoPrAar() {
        return bruttoPrAar;
    }

    public Long getDagsats() {
        return dagsats;
    }

    public Boolean getErNyIArbeidslivet() {
        return erNyIArbeidslivet;
    }

    public Boolean getErTidsbegrensetArbeidsforhold() {
        return erTidsbegrensetArbeidsforhold;
    }

    public Boolean getErTilkommetAndel() {
        return erTilkommetAndel;
    }

    public Boolean getFastsattAvSaksbehandler() {
        return fastsattAvSaksbehandler;
    }

    public BigDecimal getFastsattForrigePrAar() {
        return fastsattForrigePrAar;
    }

    public BigDecimal getFordeltPrAar() {
        return fordeltPrAar;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public Boolean getLonnsendringIBeregningsperioden() {
        return lonnsendringIBeregningsperioden;
    }

    public Long getOriginalDagsatsFraTilstøtendeYtelse() {
        return originalDagsatsFraTilstøtendeYtelse;
    }

    public BigDecimal getOverstyrtPrAar() {
        return overstyrtPrAar;
    }

    public BigDecimal getRedusertPrAar() {
        return redusertPrAar;
    }

    public Boolean getSkalFastsetteGrunnlag() {
        return skalFastsetteGrunnlag;
    }

    public void setAktivitetStatus(AktivitetStatus aktivitetStatus) {
        this.aktivitetStatus = aktivitetStatus;
    }

    public void setAndelsnr(Long andelsnr) {
        this.andelsnr = andelsnr;
    }

    public void setArbeidsforhold(BeregningsgrunnlagArbeidsforholdDto arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public void setAvkortetPrAar(BigDecimal avkortetPrAar) {
        this.avkortetPrAar = avkortetPrAar;
    }

    public void setBelopPrAarEtterAOrdningen(BigDecimal belopPrAarEtterAOrdningen) {
        this.belopPrAarEtterAOrdningen = belopPrAarEtterAOrdningen;
    }

    public void setBelopPrMndEtterAOrdningen(BigDecimal belopPrMndEtterAOrdningen) {
        this.belopPrMndEtterAOrdningen = belopPrMndEtterAOrdningen;
    }

    public void setBeregnetPrAar(BigDecimal beregnetPrAar) {
        this.beregnetPrAar = beregnetPrAar;
    }

    public void setBeregningsperiodeFom(LocalDate beregningsperiodeFom) {
        this.beregningsperiodeFom = beregningsperiodeFom;
    }

    public void setBeregningsperiodeTom(LocalDate beregningsperiodeTom) {
        this.beregningsperiodeTom = beregningsperiodeTom;
    }

    public void setBesteberegningPrAar(BigDecimal besteberegningPrAar) {
        this.besteberegningPrAar = besteberegningPrAar;
    }

    public void setBruttoPrAar(BigDecimal bruttoPrAar) {
        this.bruttoPrAar = bruttoPrAar;
    }

    public void setDagsats(Long dagsats) {
        this.dagsats = dagsats;
    }

    public void setErNyIArbeidslivet(Boolean erNyIArbeidslivet) {
        this.erNyIArbeidslivet = erNyIArbeidslivet;
    }

    public void setErTidsbegrensetArbeidsforhold(Boolean erTidsbegrensetArbeidsforhold) {
        this.erTidsbegrensetArbeidsforhold = erTidsbegrensetArbeidsforhold;
    }

    public void setErTilkommetAndel(Boolean erTilkommetAndel) {
        this.erTilkommetAndel = erTilkommetAndel;
    }

    public void setFastsattAvSaksbehandler(Boolean fastsattAvSaksbehandler) {
        this.fastsattAvSaksbehandler = fastsattAvSaksbehandler;
    }

    public void setFastsattForrigePrAar(BigDecimal fastsattForrigePrAar) {
        this.fastsattForrigePrAar = fastsattForrigePrAar;
    }

    public void setFordeltPrAar(BigDecimal fordeltPrAar) {
        this.fordeltPrAar = fordeltPrAar;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

    public void setLagtTilAvSaksbehandler(Boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public void setLonnsendringIBeregningsperioden(Boolean lonnsendringIBeregningsperioden) {
        this.lonnsendringIBeregningsperioden = lonnsendringIBeregningsperioden;
    }

    public void setOriginalDagsatsFraTilstøtendeYtelse(Long originalDagsatsFraTilstøtendeYtelse) {
        this.originalDagsatsFraTilstøtendeYtelse = originalDagsatsFraTilstøtendeYtelse;
    }

    public void setOverstyrtPrAar(BigDecimal overstyrtPrAar) {
        this.overstyrtPrAar = overstyrtPrAar;
    }

    public void setRedusertPrAar(BigDecimal redusertPrAar) {
        this.redusertPrAar = redusertPrAar;
    }

    public void setSkalFastsetteGrunnlag(Boolean skalFastsetteGrunnlag) {
        this.skalFastsetteGrunnlag = skalFastsetteGrunnlag;
    }
}
