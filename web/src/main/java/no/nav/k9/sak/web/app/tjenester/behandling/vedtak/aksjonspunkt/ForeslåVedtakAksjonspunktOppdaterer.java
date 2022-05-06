package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovListeDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.K9FormidlingKlient;
import no.nav.k9.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer implements AksjonspunktOppdaterer<ForeslaVedtakAksjonspunktDto> {

    private VedtaksbrevHåndterer vedtaksbrevHåndterer;
    private K9FormidlingKlient formidlingKlient;

    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(VedtaksbrevHåndterer vedtaksbrevHåndterer, K9FormidlingKlient formidlingKlient) {
        this.vedtaksbrevHåndterer = vedtaksbrevHåndterer;
        this.formidlingKlient = formidlingKlient;
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        vedtaksbrevHåndterer.oppdaterBegrunnelse(behandling);

        OppdateringResultat.Builder builder = OppdateringResultat.builder();
        if (Set.of(FagsakYtelseType.PSB, FagsakYtelseType.PPN).contains( behandling.getFagsakYtelseType())) {
            if (dto.isSkalBrukeOverstyrendeFritekstBrev() || trengerManueltBrev(behandling)) {
                builder.medTotrinn();
            }
        }

        vedtaksbrevHåndterer.håndterTotrinnOgHistorikkinnslag(dto, param, builder);

        vedtaksbrevHåndterer.oppdaterVedtaksvarsel(dto, param.getBehandlingId(), param.getRef().getFagsakYtelseType());

        return builder.build();
    }

    private boolean trengerManueltBrev(Behandling behandling) {
        InformasjonsbehovListeDto informasjonsbehov = formidlingKlient.hentInformasjonsbehov(behandling.getUuid(), behandling.getFagsakYtelseType());
        return !informasjonsbehov.getInformasjonsbehov().isEmpty();
    }
}
