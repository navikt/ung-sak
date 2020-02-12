package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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

    @JsonProperty(value = "aktivitetStatus", required = true)
    @Valid
    @NotNull
    @Size(max = 100)
    private List<AktivitetStatus> aktivitetStatus;

    @JsonProperty(value = "andelerMedGraderingUtenBG")
    @Size(max = 200)
    @Valid
    private List<BeregningsgrunnlagPrStatusOgAndelDto> andelerMedGraderingUtenBG = Collections.emptyList();

    @JsonProperty(value = "årsinntektVisningstall")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal årsinntektVisningstall;

    @JsonProperty(value = "beregningsgrunnlagPeriode")
    @Valid
    @Size(max = 200)
    private List<BeregningsgrunnlagPeriodeDto> beregningsgrunnlagPeriode = Collections.emptyList();

    @JsonProperty(value = "dekningsgrad")
    @Min(0)
    @Max(200)
    private int dekningsgrad;

    @JsonProperty(value = "faktaOmBeregning")
    @Valid
    private FaktaOmBeregningDto faktaOmBeregning;

    @JsonProperty(value = "faktaOmFordeling")
    @Valid
    private FordelingDto faktaOmFordeling;

    @JsonProperty(value = "grunnbeløp")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal grunnbeløp;

    @JsonProperty(value = "halvG")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal halvG;

    @JsonProperty(value = "hjemmel")
    @Valid
    private Hjemmel hjemmel;

    @JsonProperty(value = "ledetekstAvkortet")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String ledetekstAvkortet;

    @JsonProperty(value = "ledetekstBrutto")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String ledetekstBrutto;

    @JsonProperty(value = "ledetekstRedusert")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String ledetekstRedusert;

    @JsonProperty(value = "sammenligningsgrunnlag")
    @Valid
    private SammenligningsgrunnlagDto sammenligningsgrunnlag;

    @JsonProperty(value = "sammenligningsgrunnlagPrStatus")
    @Valid
    @Size(max = 200)
    private List<SammenligningsgrunnlagDto> sammenligningsgrunnlagPrStatus = Collections.emptyList();

    @JsonProperty(value = "skjaeringstidspunkt")
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "skjaeringstidspunktBeregning")
    private LocalDate skjaeringstidspunktBeregning;

    public BeregningsgrunnlagDto() {
        // trengs for deserialisering av JSON
    }

    public List<AktivitetStatus> getAktivitetStatus() {
        return aktivitetStatus;
    }

    public List<BeregningsgrunnlagPrStatusOgAndelDto> getAndelerMedGraderingUtenBG() {
        return Collections.unmodifiableList(andelerMedGraderingUtenBG);
    }

    public BigDecimal getÅrsinntektVisningstall() {
        return årsinntektVisningstall;
    }

    public List<BeregningsgrunnlagPeriodeDto> getBeregningsgrunnlagPeriode() {
        return Collections.unmodifiableList(beregningsgrunnlagPeriode);
    }

    public int getDekningsgrad() {
        return dekningsgrad;
    }

    public FaktaOmBeregningDto getFaktaOmBeregning() {
        return faktaOmBeregning;
    }

    public FordelingDto getFaktaOmFordeling() {
        return faktaOmFordeling;
    }

    public BigDecimal getGrunnbeløp() {
        return grunnbeløp;
    }

    public BigDecimal getHalvG() {
        return halvG;
    }

    public Hjemmel getHjemmel() {
        return hjemmel;
    }

    public String getLedetekstAvkortet() {
        return ledetekstAvkortet;
    }

    public String getLedetekstBrutto() {
        return ledetekstBrutto;
    }

    public String getLedetekstRedusert() {
        return ledetekstRedusert;
    }

    public SammenligningsgrunnlagDto getSammenligningsgrunnlag() {
        return sammenligningsgrunnlag;
    }

    public List<SammenligningsgrunnlagDto> getSammenligningsgrunnlagPrStatus() {
        return Collections.unmodifiableList(sammenligningsgrunnlagPrStatus);
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public LocalDate getSkjaeringstidspunktBeregning() {
        return skjaeringstidspunktBeregning;
    }

    public void setAktivitetStatus(List<AktivitetStatus> aktivitetStatus) {
        this.aktivitetStatus = List.copyOf(aktivitetStatus);
    }

    public void setAndelerMedGraderingUtenBG(List<BeregningsgrunnlagPrStatusOgAndelDto> andelerMedGraderingUtenBG) {
        this.andelerMedGraderingUtenBG = List.copyOf(andelerMedGraderingUtenBG);
    }

    public void setÅrsinntektVisningstall(BigDecimal årsinntektVisningstall) {
        this.årsinntektVisningstall = årsinntektVisningstall;
    }

    public void setBeregningsgrunnlagPeriode(List<BeregningsgrunnlagPeriodeDto> perioder) {
        this.beregningsgrunnlagPeriode = List.copyOf(perioder);
    }

    public void setDekningsgrad(int dekningsgrad) {
        this.dekningsgrad = dekningsgrad;
    }

    public void setFaktaOmBeregning(FaktaOmBeregningDto faktaOmBeregning) {
        this.faktaOmBeregning = faktaOmBeregning;
    }

    public void setFaktaOmFordeling(FordelingDto faktaOmFordelingDto) {
        this.faktaOmFordeling = faktaOmFordelingDto;
    }

    public void setGrunnbeløp(BigDecimal grunnbeløp) {
        this.grunnbeløp = grunnbeløp;
    }

    public void setHalvG(BigDecimal halvG) {
        this.halvG = halvG;
    }

    public void setHjemmel(Hjemmel hjemmel) {
        this.hjemmel = hjemmel;
    }

    public void setLedetekstAvkortet(String ledetekstAvkortet) {
        this.ledetekstAvkortet = ledetekstAvkortet;
    }

    public void setLedetekstBrutto(String ledetekstBrutto) {
        this.ledetekstBrutto = ledetekstBrutto;
    }

    public void setLedetekstRedusert(String ledetekstRedusert) {
        this.ledetekstRedusert = ledetekstRedusert;
    }

    public void setSammenligningsgrunnlag(SammenligningsgrunnlagDto sammenligningsgrunnlag) {
        this.sammenligningsgrunnlag = sammenligningsgrunnlag;
    }

    public void setSammenligningsgrunnlagPrStatus(List<SammenligningsgrunnlagDto> sammenligningsgrunnlagPrStatus) {
        this.sammenligningsgrunnlagPrStatus = List.copyOf(sammenligningsgrunnlagPrStatus);
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    public void setSkjaeringstidspunktBeregning(LocalDate skjaeringstidspunktBeregning) {
        this.skjaeringstidspunktBeregning = skjaeringstidspunktBeregning;
    }
}
