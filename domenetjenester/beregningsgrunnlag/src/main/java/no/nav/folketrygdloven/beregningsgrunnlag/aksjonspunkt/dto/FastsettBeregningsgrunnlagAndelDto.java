package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class FastsettBeregningsgrunnlagAndelDto extends RedigerbarAndelDto {

    @Valid
    @NotNull
    private FastsatteVerdierDto fastsatteVerdier;
    private Inntektskategori forrigeInntektskategori;
    private Integer forrigeRefusjonPrÅr;
    private Integer forrigeArbeidsinntektPrÅr;

    FastsettBeregningsgrunnlagAndelDto() { // NOSONAR
        // Jackson
    }

    public FastsettBeregningsgrunnlagAndelDto(RedigerbarAndelDto andelDto,
                                              FastsatteVerdierDto fastsatteVerdier, Inntektskategori forrigeInntektskategori, Integer forrigeRefusjonPrÅr, Integer forrigeArbeidsinntektPrÅr) {
        super(andelDto.getNyAndel(), andelDto.getArbeidsgiverId(), andelDto.getArbeidsforholdId(),
            andelDto.getAndelsnr(), andelDto.getLagtTilAvSaksbehandler(), andelDto.getAktivitetStatus(), OpptjeningAktivitetType.ARBEID);
        this.fastsatteVerdier = fastsatteVerdier;
        this.forrigeArbeidsinntektPrÅr = forrigeArbeidsinntektPrÅr;
        this.forrigeInntektskategori = forrigeInntektskategori;
        this.forrigeRefusjonPrÅr = forrigeRefusjonPrÅr;
    }

    public FastsatteVerdierDto getFastsatteVerdier() {
        return fastsatteVerdier;
    }

    public Inntektskategori getForrigeInntektskategori() {
        return forrigeInntektskategori;
    }

    public Integer getForrigeRefusjonPrÅr() {
        return forrigeRefusjonPrÅr;
    }

    public Integer getForrigeArbeidsinntektPrÅr() {
        return forrigeArbeidsinntektPrÅr;
    }
}
