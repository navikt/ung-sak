package no.nav.foreldrepenger.web.app.tjenester.behandling.medisinsk;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_LEGEERKLÆRING_KODE)
public class AvklarMedisinskeOpplysningerDto extends BekreftetAksjonspunktDto {

    @SuppressWarnings("unused") // NOSONAR
    private AvklarMedisinskeOpplysningerDto() {
        super();
        //For Jackson
    }

    public AvklarMedisinskeOpplysningerDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }

}
