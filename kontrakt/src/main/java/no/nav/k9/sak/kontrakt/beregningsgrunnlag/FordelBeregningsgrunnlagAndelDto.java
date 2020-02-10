package no.nav.k9.sak.kontrakt.beregningsgrunnlag;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FordelBeregningsgrunnlagAndelDto extends FaktaOmBeregningAndelDto {

    private static final int MÅNEDER_I_1_ÅR = 12;

    @JsonProperty(value="fordelingForrigeBehandlinPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal fordelingForrigeBehandlingPrAar;
    
    @JsonProperty(value="refusjonskravPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal refusjonskravPrAar = BigDecimal.ZERO;
    
    @JsonProperty(value="fordeltPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal fordeltPrAar;
    
    @JsonProperty(value="belopFraInntektsmeldingPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal belopFraInntektsmeldingPrAar;
    
    @JsonProperty(value="refusjonskravFraInntektsmeldingPrAar")
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal refusjonskravFraInntektsmeldingPrAar;

    @JsonProperty(value="nyttArbeidsforhold", required = true)
    @NotNull
    private boolean nyttArbeidsforhold;
    
    @JsonProperty(value="arbeidsforholdType", required = true)
    @Valid
    @NotNull
    private OpptjeningAktivitetType arbeidsforholdType;

    public FordelBeregningsgrunnlagAndelDto(FaktaOmBeregningAndelDto dto) {
        super(dto.getAndelsnr(), dto.getArbeidsforhold(), dto.getInntektskategori(),
            dto.getAktivitetStatus(), dto.getLagtTilAvSaksbehandler(), dto.getFastsattAvSaksbehandler(), dto.getAndelIArbeid());
    }

    public void setBelopFraInntektsmelding(BigDecimal belopFraInntektsmelding) {
        this.belopFraInntektsmeldingPrAar = belopFraInntektsmelding == null ?
            null : BigDecimal.valueOf(MÅNEDER_I_1_ÅR).multiply(belopFraInntektsmelding).setScale(0, RoundingMode.HALF_UP);
    }

    public void setFordelingForrigeBehandling(BigDecimal fordelingForrigeBehandling) {
        this.fordelingForrigeBehandlingPrAar = fordelingForrigeBehandling == null ?
            null : BigDecimal.valueOf(MÅNEDER_I_1_ÅR).multiply(fordelingForrigeBehandling).setScale(0, RoundingMode.HALF_UP);
    }

    public void setRefusjonskravPrAar(BigDecimal refusjonskravPrAar) {
        this.refusjonskravPrAar = refusjonskravPrAar == null ?
            null : refusjonskravPrAar.setScale(0, RoundingMode.HALF_UP);
    }


    public void setRefusjonskravFraInntektsmeldingPrÅr(BigDecimal refusjonskravFraInntektsmelding) {
        this.refusjonskravFraInntektsmeldingPrAar = refusjonskravFraInntektsmelding == null ?
            null : refusjonskravFraInntektsmelding.setScale(0, RoundingMode.HALF_UP);

    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public void setArbeidsforholdType(OpptjeningAktivitetType arbeidsforholdType) {
        this.arbeidsforholdType = arbeidsforholdType;
    }

    public boolean isNyttArbeidsforhold() {
        return nyttArbeidsforhold;
    }

    public void setNyttArbeidsforhold(boolean nyttArbeidsforhold) {
        this.nyttArbeidsforhold = nyttArbeidsforhold;
    }

    public BigDecimal getFordelingForrigeBehandlingPrAar() {
        return fordelingForrigeBehandlingPrAar;
    }

    public BigDecimal getRefusjonskravPrAar() {
        return refusjonskravPrAar;
    }

    public BigDecimal getBelopFraInntektsmeldingPrAar() {
        return belopFraInntektsmeldingPrAar;
    }

    public BigDecimal getRefusjonskravFraInntektsmeldingPrAar() {
        return refusjonskravFraInntektsmeldingPrAar;
    }

    public BigDecimal getFordeltPrAar() {
        return fordeltPrAar;
    }

    public void setFordeltPrAar(BigDecimal fordeltPrAar) {
        this.fordeltPrAar = fordeltPrAar;
    }
}
