package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.VurderNattevåkDto;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderNattevåkDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderNattevåk implements AksjonspunktOppdaterer<VurderNattevåkDto> {

    VurderNattevåk() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(VurderNattevåkDto dto, AksjonspunktOppdaterParameter param) {
        return null;
    }
}
