package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.vedtak.VurdereOverlappendeYteleseFørVedtakDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurdereOverlappendeYteleseFørVedtakDto.class, adapter=AksjonspunktOppdaterer.class)
class VurderOverlappendeYtelseFørVedtakOppdaterer implements AksjonspunktOppdaterer<VurdereOverlappendeYteleseFørVedtakDto> {

    @Override
    public OppdateringResultat oppdater(VurdereOverlappendeYteleseFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.utenOveropp();
    }
}
