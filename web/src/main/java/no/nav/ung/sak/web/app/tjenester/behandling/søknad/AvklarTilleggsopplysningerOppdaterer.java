package no.nav.ung.sak.web.app.tjenester.behandling.søknad;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.kontrakt.søknad.AvklarTilleggsopplysningerDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarTilleggsopplysningerDto.class, adapter=AksjonspunktOppdaterer.class)
class AvklarTilleggsopplysningerOppdaterer implements AksjonspunktOppdaterer<AvklarTilleggsopplysningerDto> {

    @Override
    public OppdateringResultat oppdater(AvklarTilleggsopplysningerDto dto, AksjonspunktOppdaterParameter param) {
        // skal ikke oppdater noe her
        return OppdateringResultat.nyttResultat();
    }
}
