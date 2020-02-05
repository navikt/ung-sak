package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto;

import java.math.BigDecimal;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

public class FastsatteVerdierForBesteberegningDto {

    private static final int MÅNEDER_I_1_ÅR = 12;

    @NotNull
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattBeløp;

    @NotNull
    private Inntektskategori inntektskategori;

    FastsatteVerdierForBesteberegningDto() { // NOSONAR
        // Jackson
    }

    public FastsatteVerdierForBesteberegningDto(Integer fastsattBeløp,
                                                Inntektskategori inntektskategori) {
        this.fastsattBeløp = fastsattBeløp;
        this.inntektskategori = inntektskategori;
    }

    public Integer getFastsattBeløp() {
        return fastsattBeløp;
    }

    public BigDecimal finnFastsattBeløpPrÅr() {
        if (fastsattBeløp == null) {
            throw new IllegalStateException("Feil under oppdatering: Månedslønn er ikke satt");
        }
        return BigDecimal.valueOf((long) fastsattBeløp * MÅNEDER_I_1_ÅR);
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Boolean getSkalHaBesteberegning() {
        return true;
    }

}
