package no.nav.foreldrepenger.web.app.tjenester.behandling.medisinsk;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

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
