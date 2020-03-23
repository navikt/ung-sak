package no.nav.k9.sak.web.app.tjenester.behandling.søknad;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.søknad.AvklarTilleggsopplysningerDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarTilleggsopplysningerDto.class, adapter=AksjonspunktOppdaterer.class)
class AvklarTilleggsopplysningerOppdaterer implements AksjonspunktOppdaterer<AvklarTilleggsopplysningerDto> {

    @Override
    public OppdateringResultat oppdater(AvklarTilleggsopplysningerDto dto, AksjonspunktOppdaterParameter param) {
        // skal ikke oppdater noe her
        return OppdateringResultat.utenOveropp();
    }
}
