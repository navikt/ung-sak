package no.nav.ung.sak.web.app.tjenester.behandling.tilbakekreving;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.kontrakt.økonomi.tilbakekreving.SjekkTilbakekrevingFørVedtakDto;
import no.nav.ung.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SjekkTilbakekrevingFørVedtakDto.class, adapter = AksjonspunktOppdaterer.class)
class VentPåTilbakekrevingFørVedtakOppdaterer implements AksjonspunktOppdaterer<SjekkTilbakekrevingFørVedtakDto> {

    private K9TilbakeRestKlient k9TilbakeRestKlient;
    private HistorikkRepository historikkRepository;

    public VentPåTilbakekrevingFørVedtakOppdaterer() {
        //for CDI proxy
    }

    @Inject
    public VentPåTilbakekrevingFørVedtakOppdaterer(K9TilbakeRestKlient k9TilbakeRestKlient,
                                                   HistorikkRepository historikkRepository) {
        this.k9TilbakeRestKlient = k9TilbakeRestKlient;
        this.historikkRepository = historikkRepository;
    }

    @Override
    public OppdateringResultat oppdater(SjekkTilbakekrevingFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        if (!k9TilbakeRestKlient.harÅpenTilbakekrevingsbehandling(param.getRef().getSaksnummer())) {
            OppdateringResultat oppdateringResultat = OppdateringResultat.nyttResultat();
            oppdateringResultat.setSteg(BehandlingStegType.SIMULER_OPPDRAG);
            oppdateringResultat.rekjørSteg();
            return oppdateringResultat;
        }

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandlingId(param.getBehandlingId());
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setType(HistorikkinnslagType.FORTSETT_UTEN_Å_VENTE_PÅ_TILBAKEKREVING);
        historikkRepository.lagre(historikkinnslag);
        return OppdateringResultat.nyttResultat();
    }
}
