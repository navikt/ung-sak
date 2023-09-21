package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class RedigerbarAndelDto {

    @JsonProperty(value = "andelsnr")
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;

    @JsonProperty(value = "lagtTilAvSaksbehandler")
    private Boolean lagtTilAvSaksbehandler;

    @JsonProperty(value = "nyAndel")
    private Boolean nyAndel;

    public RedigerbarAndelDto() {
        //
    }

    public RedigerbarAndelDto(@NotNull @Min(0) @Max(Long.MAX_VALUE) Long andelsnr, Boolean lagtTilAvSaksbehandler, Boolean nyAndel) {
        this.andelsnr = andelsnr;
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
        this.nyAndel = nyAndel;
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public Boolean getNyAndel() {
        return nyAndel;
    }

    @AssertTrue(message = "Andelsnr må vere ulik null når nyAndel er false")
    public boolean isAndelsnrNotNullWhenNotNew() {
        if (nyAndel == null || !nyAndel) {
            return andelsnr != null;
        }
        return true;
    }


}
