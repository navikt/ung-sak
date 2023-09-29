package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovListeDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.K9FormidlingKlient;
import no.nav.k9.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer implements AksjonspunktOppdaterer<ForeslaVedtakAksjonspunktDto> {

    private ForeslåVedtakOppdatererTjeneste foreslåVedtakOppdatererTjeneste;
    private FrisinnVedtaksvarselTjeneste frisinnVedtaksvarselTjeneste;
    private K9FormidlingKlient formidlingKlient;
    private boolean disableTotrinnBrev;

    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(
        ForeslåVedtakOppdatererTjeneste foreslåVedtakOppdatererTjeneste,
        FrisinnVedtaksvarselTjeneste frisinnVedtaksvarselTjeneste,
        K9FormidlingKlient formidlingKlient,
        @KonfigVerdi(value = "DISABLE_TOTRINN_BREV", defaultVerdi = "true") boolean disableTotrinnBrev) {
        this.foreslåVedtakOppdatererTjeneste = foreslåVedtakOppdatererTjeneste;
        this.frisinnVedtaksvarselTjeneste = frisinnVedtaksvarselTjeneste;
        this.formidlingKlient = formidlingKlient;
        this.disableTotrinnBrev = disableTotrinnBrev;
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        behandling.setAnsvarligSaksbehandler(SubjectHandler.getSubjectHandler().getUid());

        OppdateringResultat.Builder builder = OppdateringResultat.builder();
        if (!disableTotrinnBrev && Set.of(FagsakYtelseType.PSB, FagsakYtelseType.PPN).contains(behandling.getFagsakYtelseType())) {
            if (dto.isSkalBrukeOverstyrendeFritekstBrev() || trengerManueltBrev(behandling)) {
                builder.medTotrinn();
            }
        }

        foreslåVedtakOppdatererTjeneste.håndterTotrinnOgHistorikkinnslag(dto, param, builder);

        if (param.getRef().getFagsakYtelseType() == FagsakYtelseType.FRISINN) {
            frisinnVedtaksvarselTjeneste.oppdaterVedtaksvarsel(dto, param.getBehandlingId(), param.getRef().getFagsakYtelseType());
        }

        return builder.build();
    }

    private boolean trengerManueltBrev(Behandling behandling) {
        InformasjonsbehovListeDto informasjonsbehov = formidlingKlient.hentInformasjonsbehov(behandling.getUuid(), behandling.getFagsakYtelseType());
        return !informasjonsbehov.getInformasjonsbehov().isEmpty();
    }
}
