package no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.SjekkTilbakekrevingFørVedtakDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SjekkTilbakekrevingFørVedtakDto.class, adapter = AksjonspunktOppdaterer.class)
class VentPåTilbakekrevingFørVedtakOppdaterer implements AksjonspunktOppdaterer<SjekkTilbakekrevingFørVedtakDto> {

    @Override
    public OppdateringResultat oppdater(SjekkTilbakekrevingFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.nyttResultat();
    }
}
