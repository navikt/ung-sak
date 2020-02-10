package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.beregningsgrunnlag.Hjemmel;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BeregningsgrunnlagDto {

    @JsonProperty(value = "skjaeringstidspunktBeregning")
    private LocalDate skjaeringstidspunktBeregning;

    @JsonProperty(value = "skjaeringstidspunkt")
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "aktivitetStatus", required = true)
    @Valid
    @NotNull
    @Size(max = 100)
    private List<AktivitetStatus> aktivitetStatus;

    @JsonProperty(value = "beregningsgrunnlagPeriode")
    @Valid
    @Size(max = 200)
    private List<BeregningsgrunnlagPeriodeDto> beregningsgrunnlagPeriode;

    @JsonProperty(value = "sammenligningsgrunnlag")
    @Valid
    private SammenligningsgrunnlagDto sammenligningsgrunnlag;

    @JsonProperty(value = "sammenligningsgrunnlagPrStatus")
    @Valid
    @Size(max = 200)
    private List<SammenligningsgrunnlagDto> sammenligningsgrunnlagPrStatus;

    @JsonProperty(value = "ledetekstBrutto")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String ledetekstBrutto;

    @JsonProperty(value = "ledetekstAvkortet")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String ledetekstAvkortet;

    @JsonProperty(value = "ledetekstRedusert")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String ledetekstRedusert;

    @JsonProperty(value = "halvG")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal halvG;

    @JsonProperty(value = "grunnbeløp")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal grunnbeløp;

    @JsonProperty(value = "faktaOmBeregning")
    @Valid
    private FaktaOmBeregningDto faktaOmBeregning;

    @JsonProperty(value = "andelerMedGraderingUtenBG")
    @Size(max = 200)
    @Valid
    private List<BeregningsgrunnlagPrStatusOgAndelDto> andelerMedGraderingUtenBG;

    @JsonProperty(value = "hjemmel")
    @Valid
    private Hjemmel hjemmel;

    @JsonProperty(value = "faktaOmFordeling")
    @Valid
    private FordelingDto faktaOmFordeling;

    @JsonProperty(value = "årsinntektVisningstall")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal årsinntektVisningstall;

    @JsonProperty(value = "dekningsgrad")
    @Min(0)
    @Max(200)
    private int dekningsgrad;

    public BeregningsgrunnlagDto() {
        // trengs for deserialisering av JSON
    }

    public LocalDate getSkjaeringstidspunktBeregning() {
        return skjaeringstidspunktBeregning;
    }

    public List<AktivitetStatus> getAktivitetStatus() {
        return aktivitetStatus;
    }

    public List<BeregningsgrunnlagPeriodeDto> getBeregningsgrunnlagPeriode() {
        return beregningsgrunnlagPeriode;
    }

    public String getLedetekstBrutto() {
        return ledetekstBrutto;
    }

    public String getLedetekstAvkortet() {
        return ledetekstAvkortet;
    }

    public String getLedetekstRedusert() {
        return ledetekstRedusert;
    }

    public SammenligningsgrunnlagDto getSammenligningsgrunnlag() {
        return sammenligningsgrunnlag;
    }

    public BigDecimal getHalvG() {
        return halvG;
    }

    public FordelingDto getFaktaOmFordeling() {
        return faktaOmFordeling;
    }

    public FaktaOmBeregningDto getFaktaOmBeregning() {
        return faktaOmBeregning;
    }

    public Hjemmel getHjemmel() {
        return hjemmel;
    }

    public List<SammenligningsgrunnlagDto> getSammenligningsgrunnlagPrStatus() {
        return sammenligningsgrunnlagPrStatus;
    }

    public void setSkjaeringstidspunktBeregning(LocalDate skjaeringstidspunktBeregning) {
        this.skjaeringstidspunktBeregning = skjaeringstidspunktBeregning;
    }

    public void setAktivitetStatus(List<AktivitetStatus> aktivitetStatus) {
        this.aktivitetStatus = aktivitetStatus;
    }

    public void setBeregningsgrunnlagPeriode(List<BeregningsgrunnlagPeriodeDto> perioder) {
        this.beregningsgrunnlagPeriode = perioder;
    }

    public void setSammenligningsgrunnlag(SammenligningsgrunnlagDto sammenligningsgrunnlag) {
        this.sammenligningsgrunnlag = sammenligningsgrunnlag;
    }

    public void setLedetekstBrutto(String ledetekstBrutto) {
        this.ledetekstBrutto = ledetekstBrutto;
    }

    public void setLedetekstAvkortet(String ledetekstAvkortet) {
        this.ledetekstAvkortet = ledetekstAvkortet;
    }

    public void setLedetekstRedusert(String ledetekstRedusert) {
        this.ledetekstRedusert = ledetekstRedusert;
    }

    public void setHalvG(BigDecimal halvG) {
        this.halvG = halvG;
    }

    public void setFaktaOmBeregning(FaktaOmBeregningDto faktaOmBeregning) {
        this.faktaOmBeregning = faktaOmBeregning;
    }

    public List<BeregningsgrunnlagPrStatusOgAndelDto> getAndelerMedGraderingUtenBG() {
        return andelerMedGraderingUtenBG;
    }

    public void setAndelerMedGraderingUtenBG(List<BeregningsgrunnlagPrStatusOgAndelDto> andelerMedGraderingUtenBG) {
        this.andelerMedGraderingUtenBG = andelerMedGraderingUtenBG;
    }

    public void setHjemmel(Hjemmel hjemmel) {
        this.hjemmel = hjemmel;
    }

    public void setFaktaOmFordeling(FordelingDto faktaOmFordelingDto) {
        this.faktaOmFordeling = faktaOmFordelingDto;
    }

    public BigDecimal getÅrsinntektVisningstall() {
        return årsinntektVisningstall;
    }

    public void setÅrsinntektVisningstall(BigDecimal årsinntektVisningstall) {
        this.årsinntektVisningstall = årsinntektVisningstall;
    }

    public int getDekningsgrad() {
        return dekningsgrad;
    }

    public void setDekningsgrad(int dekningsgrad) {
        this.dekningsgrad = dekningsgrad;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    public BigDecimal getGrunnbeløp() {
        return grunnbeløp;
    }

    public void setGrunnbeløp(BigDecimal grunnbeløp) {
        this.grunnbeløp = grunnbeløp;
    }

    public void setSammenligningsgrunnlagPrStatus(List<SammenligningsgrunnlagDto> sammenligningsgrunnlagPrStatus) {
        this.sammenligningsgrunnlagPrStatus = sammenligningsgrunnlagPrStatus;
    }
}
