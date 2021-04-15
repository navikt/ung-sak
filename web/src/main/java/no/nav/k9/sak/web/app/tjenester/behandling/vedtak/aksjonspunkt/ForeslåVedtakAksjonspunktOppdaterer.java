package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer implements AksjonspunktOppdaterer<ForeslaVedtakAksjonspunktDto> {

    private VedtaksbrevHåndterer vedtaksbrevHåndterer;

    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(VedtaksbrevHåndterer vedtaksbrevHåndterer) {
        this.vedtaksbrevHåndterer = vedtaksbrevHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        String begrunnelse = dto.getBegrunnelse();
        Behandling behandling = param.getBehandling();
        vedtaksbrevHåndterer.oppdaterBegrunnelse(behandling, begrunnelse);

        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        vedtaksbrevHåndterer.oppdaterVedtaksbrev(dto, param, builder);

        vedtaksbrevHåndterer.oppdaterVedtaksvarsel(dto, param.getBehandlingId());
        return builder.build();
    }

}
