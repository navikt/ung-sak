package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FaktaOmBeregningAndelDto {

    @JsonProperty("andelsnr")
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;

    @JsonProperty("arbeidsforhold")
    @Valid
    private BeregningsgrunnlagArbeidsforholdDto arbeidsforhold;

    @JsonProperty("inntektskategori")
    @NotNull
    @Valid
    private Inntektskategori inntektskategori;

    @JsonProperty("aktivitetStatus")
    @Valid
    private AktivitetStatus aktivitetStatus;

    @JsonProperty(value = "lagtTilAvSaksbehandler", required = true)
    @NotNull
    private Boolean lagtTilAvSaksbehandler = false;

    @JsonProperty(value = "fastsattAvSaksbehandler", required = true)
    @NotNull
    private Boolean fastsattAvSaksbehandler = false;

    @JsonProperty(value = "andelIArbeid")
    @Size(max = 200)
    @Valid
    private List<@DecimalMin("0.00") @DecimalMax("500.00") @Digits(integer = 3, fraction = 2) @NotNull BigDecimal> andelIArbeid = new ArrayList<>();

    FaktaOmBeregningAndelDto(Long andelsnr, BeregningsgrunnlagArbeidsforholdDto arbeidsforhold, Inntektskategori inntektskategori,
                             AktivitetStatus aktivitetStatus, Boolean lagtTilAvSaksbehandler, Boolean fastsattAvSaksbehandler, List<BigDecimal> andelIArbeid) {
        this.andelsnr = andelsnr;
        this.arbeidsforhold = arbeidsforhold;
        this.inntektskategori = inntektskategori;
        this.aktivitetStatus = aktivitetStatus;
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
        this.fastsattAvSaksbehandler = fastsattAvSaksbehandler;
        this.andelIArbeid = andelIArbeid;
    }

    public FaktaOmBeregningAndelDto() {
        //
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FaktaOmBeregningAndelDto that = (FaktaOmBeregningAndelDto) o;
        return Objects.equals(arbeidsforhold, that.arbeidsforhold) &&
            Objects.equals(inntektskategori, that.inntektskategori) &&
            Objects.equals(aktivitetStatus, that.aktivitetStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsforhold, inntektskategori, aktivitetStatus);
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public void setAndelsnr(Long andelsnr) {
        this.andelsnr = andelsnr;
    }

    public BeregningsgrunnlagArbeidsforholdDto getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(BeregningsgrunnlagArbeidsforholdDto arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public void setAktivitetStatus(AktivitetStatus aktivitetStatus) {
        this.aktivitetStatus = aktivitetStatus;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public void setLagtTilAvSaksbehandler(Boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public Boolean getFastsattAvSaksbehandler() {
        return fastsattAvSaksbehandler;
    }

    public void setFastsattAvSaksbehandler(Boolean fastsattAvSaksbehandler) {
        this.fastsattAvSaksbehandler = fastsattAvSaksbehandler;
    }

    public List<BigDecimal> getAndelIArbeid() {
        return andelIArbeid;
    }

    public void setAndelIArbeid(List<BigDecimal> andelIArbeid) {
        this.andelIArbeid = andelIArbeid;
    }

    public void leggTilAndelIArbeid(BigDecimal andelIArbeid) {
        this.andelIArbeid.add(andelIArbeid);
    }

}
