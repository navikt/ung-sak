package no.nav.k9.sak.kontrakt.medisinsk;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_LEGEERKLÆRING_KODE)
public class AvklarMedisinskeOpplysningerDto extends BekreftetAksjonspunktDto {

    @SuppressWarnings("unused") // NOSONAR
    private AvklarMedisinskeOpplysningerDto() {
        //For Jackson
    }

    public AvklarMedisinskeOpplysningerDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }

}
