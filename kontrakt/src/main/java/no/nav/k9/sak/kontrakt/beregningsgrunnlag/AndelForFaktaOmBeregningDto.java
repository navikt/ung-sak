package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;

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
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AndelForFaktaOmBeregningDto {

    @JsonProperty(value = "aktivitetStatus")
    @Valid
    private AktivitetStatus aktivitetStatus;

    @JsonProperty(value = "andelsnr")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    @NotNull
    private Long andelsnr;

    @JsonProperty(value = "arbeidsforhold")
    @Valid
    private BeregningsgrunnlagArbeidsforholdDto arbeidsforhold;

    @JsonProperty(value = "belopReadOnly")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal belopReadOnly;

    @JsonProperty(value = "fastsattBelop")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal fastsattBelop;

    @JsonProperty(value = "inntektskategori")
    @Valid
    private Inntektskategori inntektskategori;

    @JsonProperty(value = "lagtTilAvSaksbehandler")
    private Boolean lagtTilAvSaksbehandler;

    @JsonProperty(value = "refusjonskrav")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal refusjonskrav;
    
    @JsonProperty(value = "skalKunneEndreAktivitet")
    private Boolean skalKunneEndreAktivitet;
    
    @JsonProperty(value = "visningsnavn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String visningsnavn;

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public BeregningsgrunnlagArbeidsforholdDto getArbeidsforhold() {
        return arbeidsforhold;
    }

    public BigDecimal getBelopReadOnly() {
        return belopReadOnly;
    }

    public BigDecimal getFastsattBelop() {
        return fastsattBelop;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public BigDecimal getRefusjonskrav() {
        return refusjonskrav;
    }

    public Boolean getSkalKunneEndreAktivitet() {
        return skalKunneEndreAktivitet;
    }

    public String getVisningsnavn() {
        return visningsnavn;
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

    public void setBelopReadOnly(BigDecimal belopReadOnly) {
        this.belopReadOnly = belopReadOnly;
    }

    public void setFastsattBelop(BigDecimal fastsattBelop) {
        this.fastsattBelop = fastsattBelop;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

    public void setLagtTilAvSaksbehandler(Boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public void setRefusjonskrav(BigDecimal refusjonskrav) {
        this.refusjonskrav = refusjonskrav;
    }

    public void setSkalKunneEndreAktivitet(Boolean skalKunneEndreAktivitet) {
        this.skalKunneEndreAktivitet = skalKunneEndreAktivitet;
    }

    public void setVisningsnavn(String visningsnavn) {
        this.visningsnavn = visningsnavn;
    }
}
