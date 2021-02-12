package no.nav.k9.sak.ytelse.pleiepengerbarn.medisinsk;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.AvklarMedisinskeOpplysningerDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarMedisinskeOpplysningerDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarMedisinskeOpplysninger implements AksjonspunktOppdaterer<AvklarMedisinskeOpplysningerDto> {

    AvklarMedisinskeOpplysninger() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(AvklarMedisinskeOpplysningerDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.utenOveropp();
    }

}
