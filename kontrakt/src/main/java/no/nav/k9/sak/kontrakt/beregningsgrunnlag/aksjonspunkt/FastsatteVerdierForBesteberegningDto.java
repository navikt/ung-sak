package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsatteVerdierForBesteberegningDto {

    private static final int MÅNEDER_I_1_ÅR = 12;

    @JsonProperty(value = "fastsattBeløp", required = true)
    @NotNull
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattBeløp;

    @JsonProperty(value = "inntektskategori", required = true)
    @NotNull
    private Inntektskategori inntektskategori;

    public FastsatteVerdierForBesteberegningDto() {
        //
    }

    public FastsatteVerdierForBesteberegningDto(Integer fastsattBeløp,
                                                Inntektskategori inntektskategori) {
        this.fastsattBeløp = fastsattBeløp;
        this.inntektskategori = inntektskategori;
    }

    public BigDecimal finnFastsattBeløpPrÅr() {
        if (fastsattBeløp == null) {
            throw new IllegalStateException("Feil under oppdatering: Månedslønn er ikke satt");
        }
        return BigDecimal.valueOf((long) fastsattBeløp * MÅNEDER_I_1_ÅR);
    }

    public Integer getFastsattBeløp() {
        return fastsattBeløp;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Boolean getSkalHaBesteberegning() {
        return true;
    }

    public void setFastsattBeløp(Integer fastsattBeløp) {
        this.fastsattBeløp = fastsattBeløp;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

}
