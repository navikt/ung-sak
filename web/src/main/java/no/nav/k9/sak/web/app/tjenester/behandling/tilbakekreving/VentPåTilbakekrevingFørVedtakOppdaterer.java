package no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.SjekkTilbakekrevingFørVedtakDto;
import no.nav.k9.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SjekkTilbakekrevingFørVedtakDto.class, adapter = AksjonspunktOppdaterer.class)
class VentPåTilbakekrevingFørVedtakOppdaterer implements AksjonspunktOppdaterer<SjekkTilbakekrevingFørVedtakDto> {

    private K9TilbakeRestKlient k9TilbakeRestKlient;

    public VentPåTilbakekrevingFørVedtakOppdaterer() {
        //for CDI proxy
    }

    @Inject
    public VentPåTilbakekrevingFørVedtakOppdaterer(K9TilbakeRestKlient k9TilbakeRestKlient) {
        this.k9TilbakeRestKlient = k9TilbakeRestKlient;
    }

    @Override
    public OppdateringResultat oppdater(SjekkTilbakekrevingFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        if (!k9TilbakeRestKlient.harÅpenTilbakekrevingsbehandling(param.getRef().getSaksnummer())) {
            OppdateringResultat oppdateringResultat = OppdateringResultat.nyttResultat();
            oppdateringResultat.setSteg(BehandlingStegType.SIMULER_OPPDRAG);
            oppdateringResultat.rekjørSteg();
            return oppdateringResultat;
        }
        return OppdateringResultat.nyttResultat();
    }
}
