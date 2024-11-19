package no.nav.ung.sak.kontrakt.søknad;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER_KODE)
public class AvklarTilleggsopplysningerDto extends BekreftetAksjonspunktDto {

    public AvklarTilleggsopplysningerDto() {
        // For Jackson
    }

    public AvklarTilleggsopplysningerDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }

}
