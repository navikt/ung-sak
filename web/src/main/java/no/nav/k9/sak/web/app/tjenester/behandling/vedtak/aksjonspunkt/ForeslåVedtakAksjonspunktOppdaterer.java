package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer implements AksjonspunktOppdaterer<ForeslaVedtakAksjonspunktDto> {

    private ForeslåVedtakOppdatererTjeneste foreslåVedtakOppdatererTjeneste;
    private FrisinnVedtaksvarselTjeneste frisinnVedtaksvarselTjeneste;

    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(
        ForeslåVedtakOppdatererTjeneste foreslåVedtakOppdatererTjeneste,
        FrisinnVedtaksvarselTjeneste frisinnVedtaksvarselTjeneste) {
        this.foreslåVedtakOppdatererTjeneste = foreslåVedtakOppdatererTjeneste;
        this.frisinnVedtaksvarselTjeneste = frisinnVedtaksvarselTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        behandling.setAnsvarligSaksbehandler(SubjectHandler.getSubjectHandler().getUid());

        OppdateringResultat.Builder builder = OppdateringResultat.builder();

        foreslåVedtakOppdatererTjeneste.håndterTotrinnOgHistorikkinnslag(dto, param, builder);

        if (param.getRef().getFagsakYtelseType() == FagsakYtelseType.FRISINN) {
            frisinnVedtaksvarselTjeneste.oppdaterVedtaksvarsel(dto, param.getBehandlingId(), param.getRef().getFagsakYtelseType());
        }

        return builder.build();
    }

}
