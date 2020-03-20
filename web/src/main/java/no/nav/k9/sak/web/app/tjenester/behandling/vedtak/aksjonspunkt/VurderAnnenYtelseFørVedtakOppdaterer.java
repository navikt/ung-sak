package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.vedtak.VurdereAnnenYteleseFørVedtakDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurdereAnnenYteleseFørVedtakDto.class, adapter=AksjonspunktOppdaterer.class)
class VurderAnnenYtelseFørVedtakOppdaterer implements AksjonspunktOppdaterer<VurdereAnnenYteleseFørVedtakDto> {

    @Override
    public OppdateringResultat oppdater(VurdereAnnenYteleseFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.utenOveropp();
    }
}
