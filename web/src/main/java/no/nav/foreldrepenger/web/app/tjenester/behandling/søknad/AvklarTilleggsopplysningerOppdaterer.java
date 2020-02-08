package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.dto.AvklarTilleggsopplysningerDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarTilleggsopplysningerDto.class, adapter=AksjonspunktOppdaterer.class)
class AvklarTilleggsopplysningerOppdaterer implements AksjonspunktOppdaterer<AvklarTilleggsopplysningerDto> {

    @Override
    public OppdateringResultat oppdater(AvklarTilleggsopplysningerDto dto, AksjonspunktOppdaterParameter param) {
        // skal ikke oppdater noe her
        return OppdateringResultat.utenOveropp();
    }
}
