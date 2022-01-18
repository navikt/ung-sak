package no.nav.k9.sak.kontrakt.beregningsresultat;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_TILKJENT_YTELSE_KODE)
public class BekreftTilkjentYtelseDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "tilkjentYtelse")
    @Valid
    TilkjentYtelseDto tilkjentYtelseDto;

    public BekreftTilkjentYtelseDto() {
        // For Jackson
    }

    public TilkjentYtelseDto getTilkjentYtelse() {
        return tilkjentYtelseDto;
    }

    public void setTilkjentYtelseDto(TilkjentYtelseDto tilkjentYtelseDto) {
        this.tilkjentYtelseDto = tilkjentYtelseDto;
    }

}
