package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsattBrukersAndel {

    @JsonProperty(value = "andelsnr", required = true)
    @Min(0)
    @Max(Long.MAX_VALUE)
    @NotNull
    private Long andelsnr;
    
    @JsonProperty(value = "fastsattBeløp")
    @NotNull
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattBeløp;
    
    @JsonProperty(value = "inntektskategori", required = true)
    @NotNull
    private Inntektskategori inntektskategori;
    
    @JsonProperty(value = "lagtTilAvSaksbehandler")
    private Boolean lagtTilAvSaksbehandler;
    
    @JsonProperty(value = "nyAndel", required = true)
    @NotNull
    private Boolean nyAndel;

    public FastsattBrukersAndel() {
        //
    }

    public FastsattBrukersAndel(Boolean nyAndel,
                                Long andelsnr,
                                Boolean lagtTilAvSaksbehandler,
                                Integer fastsattBeløp,
                                Inntektskategori inntektskategori) {
        this.fastsattBeløp = fastsattBeløp;
        this.inntektskategori = inntektskategori;
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
        this.andelsnr = andelsnr;
        this.nyAndel = nyAndel;
    }


    public Long getAndelsnr() {
        return andelsnr;
    }

    public Integer getFastsattBeløp() {
        return fastsattBeløp;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public Boolean getNyAndel() {
        return nyAndel;
    }

    public void setAndelsnr(Long andelsnr) {
        this.andelsnr = andelsnr;
    }

    public void setFastsattBeløp(Integer fastsattBeløp) {
        this.fastsattBeløp = fastsattBeløp;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

    public void setLagtTilAvSaksbehandler(Boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public void setNyAndel(Boolean nyAndel) {
        this.nyAndel = nyAndel;
    }
}
