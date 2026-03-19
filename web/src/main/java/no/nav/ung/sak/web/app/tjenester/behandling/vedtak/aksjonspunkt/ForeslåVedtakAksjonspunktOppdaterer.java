package no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.domene.vedtak.OppdaterAnsvarligSaksbehandlerTjeneste;
import no.nav.ung.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;

import java.util.Set;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer implements AksjonspunktOppdaterer<ForeslaVedtakAksjonspunktDto> {

    private ForeslåVedtakOppdatererTjeneste foreslåVedtakOppdatererTjeneste;
    private Instance<OppdaterAnsvarligSaksbehandlerTjeneste> oppdaterAnsvarligSaksbehandlerTjenester;

    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(ForeslåVedtakOppdatererTjeneste foreslåVedtakOppdatererTjeneste,
                                               @Any Instance<OppdaterAnsvarligSaksbehandlerTjeneste> oppdaterAnsvarligSaksbehandlerTjenester) {
        this.foreslåVedtakOppdatererTjeneste = foreslåVedtakOppdatererTjeneste;
        this.oppdaterAnsvarligSaksbehandlerTjenester = oppdaterAnsvarligSaksbehandlerTjenester;
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        OppdaterAnsvarligSaksbehandlerTjeneste oppdaterAnsvarligSaksbehandlerTjeneste = OppdaterAnsvarligSaksbehandlerTjeneste.finnTjeneste(oppdaterAnsvarligSaksbehandlerTjenester, param.getRef().getFagsakYtelseType());
        oppdaterAnsvarligSaksbehandlerTjeneste.oppdaterAnsvarligSaksbehandler(Set.of(dto), param.getBehandlingId());

        OppdateringResultat.Builder builder = OppdateringResultat.builder();

        foreslåVedtakOppdatererTjeneste.håndterTotrinnOgHistorikkinnslag(dto, param, builder);

        return builder.build();
    }

}
