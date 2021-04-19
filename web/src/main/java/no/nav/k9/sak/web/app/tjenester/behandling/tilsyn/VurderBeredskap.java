package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.VurderBeredskapDto;
import no.nav.k9.sak.kontrakt.tilsyn.aksjonspunkt.VurderNattevåkDto;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderBeredskapDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderBeredskap implements AksjonspunktOppdaterer<VurderBeredskapDto> {

    VurderBeredskap() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(VurderBeredskapDto dto, AksjonspunktOppdaterParameter param) {
        return null;
    }
}
