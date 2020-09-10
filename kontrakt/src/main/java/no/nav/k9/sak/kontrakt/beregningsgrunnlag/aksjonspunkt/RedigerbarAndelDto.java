package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.time.LocalDate;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class RedigerbarAndelDto {

    @JsonProperty(value = "andelsnr", required = true)
    @NotNull
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


}
