package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.kontrakt.vedtak.ForeslaVedtakManueltAksjonspuntDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakManueltAksjonspuntDto.class, adapter=AksjonspunktOppdaterer.class)
class ForeslåVedtakManueltAksjonspunktOppdaterer implements AksjonspunktOppdaterer<ForeslaVedtakManueltAksjonspuntDto> {

    private VedtaksbrevHåndterer vedtaksbrevHåndterer;

    ForeslåVedtakManueltAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakManueltAksjonspunktOppdaterer(VedtaksbrevHåndterer vedtaksbrevHåndterer) {
        this.vedtaksbrevHåndterer = vedtaksbrevHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakManueltAksjonspuntDto dto, AksjonspunktOppdaterParameter param) {
        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            vedtaksbrevHåndterer.oppdaterVedtaksbrev(dto, param, builder);
            builder.medFremoverHopp(FellesTransisjoner.FREMHOPP_TIL_FATTE_VEDTAK);
        }
        vedtaksbrevHåndterer.oppdaterVedtaksvarsel(dto, param.getBehandlingId());
        return builder.build();
    }
}
