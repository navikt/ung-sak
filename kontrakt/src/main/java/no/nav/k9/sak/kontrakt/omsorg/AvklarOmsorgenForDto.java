package no.nav.k9.sak.kontrakt.omsorg;

import java.util.ArrayList;
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
@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OMSORGEN_FOR_KODE_V2)
public class AvklarOmsorgenForDto extends BekreftetAksjonspunktDto {
    
    @JsonProperty(value = "omsorgsperioder")
    @Size(max = 1000)
    @Valid
    private List<OmsorgenForOppdateringDto> omsorgsperioder = new ArrayList<>();

    
    public AvklarOmsorgenForDto(String begrunnelse, List<OmsorgenForOppdateringDto> omsorgsperioder) {
        super(begrunnelse);
        this.omsorgsperioder = omsorgsperioder;
    }

    protected AvklarOmsorgenForDto() {
        //
    }


    public List<OmsorgenForOppdateringDto> getOmsorgsperioder() {
        return omsorgsperioder;
    }
}
