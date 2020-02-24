package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.math.BigDecimal;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsatteVerdierDto {

    private static final int MÅNEDER_I_1_ÅR = 12;

    @JsonProperty(value = "fastsattÅrsbeløp")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattÅrsbeløp;

    @JsonProperty(value = "fastsattBeløp")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattBeløp;

    @JsonProperty(value = "inntektskategori")
    @Valid
    private Inntektskategori inntektskategori;

    @JsonProperty(value = "refusjon")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer refusjon;

    @JsonProperty(value = "refusjonPrÅr")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer refusjonPrÅr;

    @JsonProperty(value = "skalhaBesteberegning")
    private Boolean skalHaBesteberegning;

    public FastsatteVerdierDto() {
        //
    }

    public FastsatteVerdierDto(Integer fastsattBeløp) {
        this.fastsattBeløp = fastsattBeløp;
        this.fastsattÅrsbeløp = fastsattBeløp * MÅNEDER_I_1_ÅR;
    }

    public FastsatteVerdierDto(Integer fastsattBeløp, Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
        this.fastsattBeløp = fastsattBeløp;
        this.fastsattÅrsbeløp = fastsattBeløp * MÅNEDER_I_1_ÅR;
    }

    public FastsatteVerdierDto(Integer fastsattÅrsbeløp,
                               Inntektskategori inntektskategori,
                               Boolean skalHaBesteberegning) {
        this.fastsattBeløp = fastsattÅrsbeløp / MÅNEDER_I_1_ÅR;
        this.fastsattÅrsbeløp = fastsattÅrsbeløp;
        this.inntektskategori = inntektskategori;
        this.skalHaBesteberegning = skalHaBesteberegning;
    }

    public FastsatteVerdierDto(Integer refusjon,
                               Integer fastsattBeløp,
                               Inntektskategori inntektskategori,
                               Boolean skalHaBesteberegning) {
        this.refusjon = refusjon;
        this.refusjonPrÅr = refusjon == null ? null : refusjon * MÅNEDER_I_1_ÅR;
        this.fastsattBeløp = fastsattBeløp;
        this.inntektskategori = inntektskategori;
        this.skalHaBesteberegning = skalHaBesteberegning;
    }

    public BigDecimal finnEllerUtregnFastsattBeløpPrÅr() {
        if (fastsattÅrsbeløp != null) {
            return BigDecimal.valueOf(fastsattÅrsbeløp);
        }
        if (fastsattBeløp == null) {
            throw new IllegalStateException("Feil under oppdatering: Hverken årslønn eller månedslønn er satt.");
        }
        return BigDecimal.valueOf((long) fastsattBeløp * MÅNEDER_I_1_ÅR);
    }

    public Integer getFastsattÅrsbeløp() {
        return fastsattÅrsbeløp;
    }

    public Integer getFastsattBeløp() {
        return fastsattBeløp;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Integer getRefusjon() {
        return refusjon;
    }

    public Integer getRefusjonPrÅr() {
        if (refusjonPrÅr != null) {
            return refusjonPrÅr;
        }
        return refusjon == null ? null : refusjon * MÅNEDER_I_1_ÅR;
    }

    public Boolean getSkalHaBesteberegning() {
        return skalHaBesteberegning;
    }

    public void setFastsattÅrsbeløp(Integer fastsattÅrsbeløp) {
        this.fastsattÅrsbeløp = fastsattÅrsbeløp;
    }

    public void setFastsattBeløp(Integer fastsattBeløp) {
        this.fastsattBeløp = fastsattBeløp;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

    public void setRefusjon(Integer refusjon) {
        this.refusjon = refusjon;
    }

    public void setRefusjonPrÅr(Integer refusjonPrÅr) {
        this.refusjonPrÅr = refusjonPrÅr;
    }

    public void setSkalHaBesteberegning(Boolean skalHaBesteberegning) {
        this.skalHaBesteberegning = skalHaBesteberegning;
    }
}
