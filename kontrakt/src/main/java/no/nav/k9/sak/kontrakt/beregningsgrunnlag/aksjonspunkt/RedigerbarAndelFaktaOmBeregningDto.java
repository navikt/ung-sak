package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;

/**
 * For å kunne identifisere andeler i fakta om beregning.
 *
 * Enten andelsnr eller aktivitetstatus må vere satt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class RedigerbarAndelFaktaOmBeregningDto {

    @JsonProperty(value = "andelsnr", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;

    @JsonProperty(value = "nyAndel")
    private Boolean nyAndel;

    @JsonProperty(value = "aktivitetStatus")
    @Valid
    private AktivitetStatus aktivitetStatus;

    @JsonProperty(value = "lagtTilAvSaksbehandler")
    private Boolean lagtTilAvSaksbehandler;

    protected RedigerbarAndelFaktaOmBeregningDto() {
        //
    }

    public RedigerbarAndelFaktaOmBeregningDto(AktivitetStatus aktivitetStatus) {
        this.nyAndel = true;
        this.lagtTilAvSaksbehandler = true;
        this.aktivitetStatus = aktivitetStatus;
    }

    public RedigerbarAndelFaktaOmBeregningDto(Boolean nyAndel,
                                              long andelsnr,
                                              Boolean lagtTilAvSaksbehandler) {
        this.nyAndel = nyAndel;
        this.andelsnr = andelsnr;
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public Optional<AktivitetStatus> getAktivitetStatus() {
        return Optional.ofNullable(aktivitetStatus);
    }

    public Optional<Long> getAndelsnr() {
        return Optional.ofNullable(andelsnr);
    }

    public Boolean getNyAndel() {
        return nyAndel;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

}
