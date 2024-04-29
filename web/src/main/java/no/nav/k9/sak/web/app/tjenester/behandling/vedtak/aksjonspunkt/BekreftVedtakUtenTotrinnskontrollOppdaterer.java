package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.vedtak.BekreftVedtakUtenTotrinnskontrollDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftVedtakUtenTotrinnskontrollDto.class, adapter = AksjonspunktOppdaterer.class)
class BekreftVedtakUtenTotrinnskontrollOppdaterer implements AksjonspunktOppdaterer<BekreftVedtakUtenTotrinnskontrollDto> {

    private ForeslåVedtakOppdatererTjeneste foreslåVedtakOppdatererTjeneste;
    private FrisinnVedtaksvarselTjeneste frisinnVedtaksvarselTjeneste;

    BekreftVedtakUtenTotrinnskontrollOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftVedtakUtenTotrinnskontrollOppdaterer(ForeslåVedtakOppdatererTjeneste foreslåVedtakOppdatererTjeneste, FrisinnVedtaksvarselTjeneste frisinnVedtaksvarselTjeneste) {
        this.foreslåVedtakOppdatererTjeneste = foreslåVedtakOppdatererTjeneste;
        this.frisinnVedtaksvarselTjeneste = frisinnVedtaksvarselTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftVedtakUtenTotrinnskontrollDto dto, AksjonspunktOppdaterParameter param) {
        OppdateringResultat.Builder builder = OppdateringResultat.builder();
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            foreslåVedtakOppdatererTjeneste.håndterTotrinnOgHistorikkinnslag(dto, param, builder);
        }
        if (param.getRef().getFagsakYtelseType() == FagsakYtelseType.FRISINN) {
            frisinnVedtaksvarselTjeneste.oppdaterVedtaksvarsel(dto, param.getBehandlingId(), param.getRef().getFagsakYtelseType());
        }
        return builder.build();
    }
}
