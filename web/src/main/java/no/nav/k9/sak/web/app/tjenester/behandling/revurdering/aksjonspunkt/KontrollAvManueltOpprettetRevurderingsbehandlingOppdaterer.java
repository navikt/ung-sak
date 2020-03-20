package no.nav.k9.sak.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.behandling.revurdering.KontrollAvManueltOpprettetRevurderingsbehandlingDto;


@ApplicationScoped
@DtoTilServiceAdapter(dto = KontrollAvManueltOpprettetRevurderingsbehandlingDto.class, adapter = AksjonspunktOppdaterer.class)
class KontrollAvManueltOpprettetRevurderingsbehandlingOppdaterer implements AksjonspunktOppdaterer<KontrollAvManueltOpprettetRevurderingsbehandlingDto> {

    @Override
    public OppdateringResultat oppdater(KontrollAvManueltOpprettetRevurderingsbehandlingDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.utenOveropp();
    }

}


