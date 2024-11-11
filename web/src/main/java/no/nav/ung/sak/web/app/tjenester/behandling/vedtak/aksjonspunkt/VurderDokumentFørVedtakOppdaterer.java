package no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.kontrakt.vedtak.VurdereDokumentFørVedtakDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurdereDokumentFørVedtakDto.class, adapter=AksjonspunktOppdaterer.class)
class VurderDokumentFørVedtakOppdaterer implements AksjonspunktOppdaterer<VurdereDokumentFørVedtakDto> {

    @Override
    public OppdateringResultat oppdater(VurdereDokumentFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.nyttResultat();
    }
}
