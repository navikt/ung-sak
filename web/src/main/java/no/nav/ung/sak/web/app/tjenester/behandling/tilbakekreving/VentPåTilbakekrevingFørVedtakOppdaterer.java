package no.nav.ung.sak.web.app.tjenester.behandling.tilbakekreving;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.kontrakt.økonomi.tilbakekreving.SjekkTilbakekrevingFørVedtakDto;
import no.nav.ung.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SjekkTilbakekrevingFørVedtakDto.class, adapter = AksjonspunktOppdaterer.class)
class VentPåTilbakekrevingFørVedtakOppdaterer implements AksjonspunktOppdaterer<SjekkTilbakekrevingFørVedtakDto> {

    private K9TilbakeRestKlient k9TilbakeRestKlient;
    private HistorikkinnslagRepository historikkinnslagRepository;

    public VentPåTilbakekrevingFørVedtakOppdaterer() {
        //for CDI proxy
    }

    @Inject
    public VentPåTilbakekrevingFørVedtakOppdaterer(K9TilbakeRestKlient k9TilbakeRestKlient,
                                                   HistorikkinnslagRepository historikkinnslagRepository) {
        this.k9TilbakeRestKlient = k9TilbakeRestKlient;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(SjekkTilbakekrevingFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        if (!k9TilbakeRestKlient.harÅpenTilbakekrevingsbehandling(param.getRef().getSaksnummer())) {
            OppdateringResultat oppdateringResultat = OppdateringResultat.nyttResultat();
            oppdateringResultat.setSteg(BehandlingStegType.SIMULER_OPPDRAG);
            oppdateringResultat.rekjørSteg();
            return oppdateringResultat;
        }

        var historikkinnslag = new Historikkinnslag.Builder()
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(param.getRef().getFagsakId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_SIMULERING)
            .addLinje("Fortsetter behandlingen uten å behandle tilbakekrevingsaken først").build();
        historikkinnslagRepository.lagre(historikkinnslag);
        return OppdateringResultat.nyttResultat();
    }
}
