package no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.kontrakt.vedtak.VurdereOverlappendeYteleseFørVedtakDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurdereOverlappendeYteleseFørVedtakDto.class, adapter=AksjonspunktOppdaterer.class)
class VurderOverlappendeYtelseFørVedtakOppdaterer implements AksjonspunktOppdaterer<VurdereOverlappendeYteleseFørVedtakDto> {

    @Override
    public OppdateringResultat oppdater(VurdereOverlappendeYteleseFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.nyttResultat();
    }
}
