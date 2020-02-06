package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.vedtak.VurdereDokumentFørVedtakDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurdereDokumentFørVedtakDto.class, adapter=AksjonspunktOppdaterer.class)
class VurderDokumentFørVedtakOppdaterer implements AksjonspunktOppdaterer<VurdereDokumentFørVedtakDto> {

    @Override
    public OppdateringResultat oppdater(VurdereDokumentFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.utenOveropp();
    }
}
