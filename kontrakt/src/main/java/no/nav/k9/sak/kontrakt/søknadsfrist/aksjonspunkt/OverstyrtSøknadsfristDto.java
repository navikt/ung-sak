package no.nav.k9.sak.kontrakt.søknadsfrist.aksjonspunkt;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.OverstyringAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE)
public class OverstyrtSøknadsfristDto extends OverstyringAksjonspunktDto {

    @Valid
    @NotNull
    @Size(min = 1)
    @JsonProperty(value = "avklarteKrav")
    private List<AvklartKravDto> avklarteKrav;

    public OverstyrtSøknadsfristDto() {
    }

    public OverstyrtSøknadsfristDto(String begrunnelse, List<AvklartKravDto> avklarteKrav) {
        super(begrunnelse);
        this.avklarteKrav = avklarteKrav;
    }

    public List<AvklartKravDto> getAvklarteKrav() {
        return avklarteKrav;
    }

    @Override
    public String getAvslagskode() {
        return null;
    }

    @Override
    public boolean getErVilkarOk() {
        return false;
    }
}
