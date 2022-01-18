package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.kontrakt.vedtak.BekreftVedtakUtenTotrinnskontrollDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftVedtakUtenTotrinnskontrollDto.class, adapter = AksjonspunktOppdaterer.class)
class BekreftVedtakUtenTotrinnskontrollOppdaterer implements AksjonspunktOppdaterer<BekreftVedtakUtenTotrinnskontrollDto> {

    private VedtaksbrevHåndterer vedtaksbrevHåndterer;

    BekreftVedtakUtenTotrinnskontrollOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftVedtakUtenTotrinnskontrollOppdaterer(VedtaksbrevHåndterer vedtaksbrevHåndterer) {
        this.vedtaksbrevHåndterer = vedtaksbrevHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(BekreftVedtakUtenTotrinnskontrollDto dto, AksjonspunktOppdaterParameter param) {
        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            vedtaksbrevHåndterer.oppdaterVedtaksbrev(dto, param, builder);
            builder.medFremoverHopp(FellesTransisjoner.FREMHOPP_TIL_FATTE_VEDTAK);
        }

        vedtaksbrevHåndterer.oppdaterVedtaksvarsel(dto, param.getBehandlingId());
        return builder.build();
    }
}
