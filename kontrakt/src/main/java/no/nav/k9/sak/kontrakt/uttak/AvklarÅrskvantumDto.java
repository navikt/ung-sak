package no.nav.k9.sak.kontrakt.uttak;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_ÅRSKVANTUM_KVOTE)
public class AvklarÅrskvantumDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "harNyeRammevedtakIInfotrygd", required = true)
    @NotNull
    private Boolean harNyeRammevedtakIInfotrygd;

    public AvklarÅrskvantumDto(String begrunnelse, @JsonProperty(value = "harNyeRammevedtakIInfotrygd", required = true) @NotNull Boolean harNyeRammevedtakIInfotrygd) {
        super(begrunnelse);
        this.harNyeRammevedtakIInfotrygd = harNyeRammevedtakIInfotrygd;
    }

    protected AvklarÅrskvantumDto() {
        //
    }

    public Boolean getHarNyeRammevedtakIInfotrygd() {
        return harNyeRammevedtakIInfotrygd;
    }
}
