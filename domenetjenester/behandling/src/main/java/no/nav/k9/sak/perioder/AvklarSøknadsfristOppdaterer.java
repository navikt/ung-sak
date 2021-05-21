package no.nav.k9.sak.perioder;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.søknadsfrist.aksjonspunkt.AvklarSøknadsfristDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarSøknadsfristDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarSøknadsfristOppdaterer implements AksjonspunktOppdaterer<AvklarSøknadsfristDto> {

    @Override
    public OppdateringResultat oppdater(AvklarSøknadsfristDto dto, AksjonspunktOppdaterParameter param) {
        return null;
    }
}
