package no.nav.k9.sak.kontrakt.beregningsresultat;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNING_KODE)
public class OverstyringBeregningDto extends OverstyringAksjonspunktDto {

    @JsonProperty(value = "beregnetTilkjentYtelse")
    @Min(0)
    @Max(1000000L)
    private long beregnetTilkjentYtelse;

    public OverstyringBeregningDto() {
        //
    }

    public OverstyringBeregningDto(long beregnetTilkjentYtelse, String begrunnelse, @Valid @NotNull Periode periode) { // NOSONAR
        super(periode, begrunnelse);
        this.beregnetTilkjentYtelse = beregnetTilkjentYtelse;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    public long getBeregnetTilkjentYtelse() {
        return beregnetTilkjentYtelse;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        return true;
    }

    public void setBeregnetTilkjentYtelse(long beregnetTilkjentYtelse) {
        this.beregnetTilkjentYtelse = beregnetTilkjentYtelse;
    }
}
