package no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer implements AksjonspunktOppdaterer<ForeslaVedtakAksjonspunktDto> {

    private ForeslåVedtakOppdatererTjeneste foreslåVedtakOppdatererTjeneste;
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(
        ForeslåVedtakOppdatererTjeneste foreslåVedtakOppdatererTjeneste, BehandlingAnsvarligRepository behandlingAnsvarligRepository) {
        this.foreslåVedtakOppdatererTjeneste = foreslåVedtakOppdatererTjeneste;
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        behandlingAnsvarligRepository.setAnsvarligSaksbehandler(param.getBehandlingId(), SubjectHandler.getSubjectHandler().getUid());

        OppdateringResultat.Builder builder = OppdateringResultat.builder();

        foreslåVedtakOppdatererTjeneste.håndterTotrinnOgHistorikkinnslag(dto, param, builder);

        return builder.build();
    }

}
