package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
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
        OppdateringResultat.Builder builder = OppdateringResultat.builder();
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            vedtaksbrevHåndterer.håndterTotrinnOgHistorikkinnslag(dto, param, builder);
        }
        vedtaksbrevHåndterer.oppdaterVedtaksvarsel(dto, param.getBehandlingId(), param.getRef().getFagsakYtelseType());
        return builder.build();
    }
}
