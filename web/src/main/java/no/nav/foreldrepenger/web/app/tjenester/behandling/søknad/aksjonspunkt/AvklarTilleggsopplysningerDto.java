package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER_KODE)
public class AvklarTilleggsopplysningerDto extends BekreftetAksjonspunktDto {


    @SuppressWarnings("unused") // NOSONAR
    private AvklarTilleggsopplysningerDto() {
        super();
        //For Jackson
    }

    public AvklarTilleggsopplysningerDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }


}
