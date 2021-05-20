package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.vedtak.VurdereManueltBrevDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurdereManueltBrevDto.class, adapter = AksjonspunktOppdaterer.class)
class VurderManueltBrevOppdaterer implements AksjonspunktOppdaterer<VurdereManueltBrevDto> {

    @Override
    public OppdateringResultat oppdater(VurdereManueltBrevDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.utenOveropp();
    }
}
