package no.nav.k9.sak.kontrakt.vedtak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

    @JsonProperty(value = "aksjonspunktGodkjenningDtos")
    @Valid
    @Size(max = 20)
    private Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos = new ArrayList<>();

    public FatterVedtakAksjonspunktDto() {
        // For Jackson
    }

    public FatterVedtakAksjonspunktDto(String begrunnelse, Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos) {
        super(begrunnelse);
        this.aksjonspunktGodkjenningDtos = List.copyOf(aksjonspunktGodkjenningDtos);
    }

    public Collection<AksjonspunktGodkjenningDto> getAksjonspunktGodkjenningDtos() {
        return Collections.unmodifiableCollection(aksjonspunktGodkjenningDtos);
    }

    public void setAksjonspunktGodkjenningDtos(Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos) {
        this.aksjonspunktGodkjenningDtos = aksjonspunktGodkjenningDtos;
    }
}
