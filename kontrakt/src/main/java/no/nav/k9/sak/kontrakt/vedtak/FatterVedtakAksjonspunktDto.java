package no.nav.k9.sak.kontrakt.vedtak;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.Size;

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
@JsonTypeName(AksjonspunktKodeDefinisjon.FATTER_VEDTAK_KODE)
public class FatterVedtakAksjonspunktDto extends BekreftetAksjonspunktDto {
    
    @JsonProperty(value="aksjonspunktGodkjenningDtos")
    @Valid
    @Size(max = 20)
    private Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos;

    FatterVedtakAksjonspunktDto() {
        // For Jackson
    }

    public FatterVedtakAksjonspunktDto(String begrunnelse, Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos) {
        super(begrunnelse);
        this.aksjonspunktGodkjenningDtos = aksjonspunktGodkjenningDtos;
    }


    public Collection<AksjonspunktGodkjenningDto> getAksjonspunktGodkjenningDtos() {
        return aksjonspunktGodkjenningDtos;
    }
}
