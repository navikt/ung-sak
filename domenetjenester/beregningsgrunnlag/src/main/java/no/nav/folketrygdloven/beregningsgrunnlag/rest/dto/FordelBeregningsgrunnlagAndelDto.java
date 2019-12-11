package no.nav.folketrygdloven.beregningsgrunnlag.rest.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;

public class FordelBeregningsgrunnlagAndelDto extends FaktaOmBeregningAndelDto {

    private static final int MÅNEDER_I_1_ÅR = 12;
    private BigDecimal fordelingForrigeBehandlingPrAar;
    private BigDecimal refusjonskravPrAar = BigDecimal.ZERO;
    private BigDecimal fordeltPrAar;
    private BigDecimal belopFraInntektsmeldingPrAar;
    private BigDecimal refusjonskravFraInntektsmeldingPrAar;
    private boolean nyttArbeidsforhold;
    private OpptjeningAktivitetType arbeidsforholdType;

    public FordelBeregningsgrunnlagAndelDto(FaktaOmBeregningAndelDto superDto) {
        super(superDto.getAndelsnr(), superDto.getArbeidsforhold(), superDto.getInntektskategori(),
            superDto.getAktivitetStatus(), superDto.getLagtTilAvSaksbehandler(), superDto.getFastsattAvSaksbehandler(), superDto.getAndelIArbeid());
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
