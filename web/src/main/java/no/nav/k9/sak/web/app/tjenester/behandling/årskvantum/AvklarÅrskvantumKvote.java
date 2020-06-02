package no.nav.k9.sak.web.app.tjenester.behandling.årskvantum;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.uttak.AvklarÅrskvantumDto;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarÅrskvantumDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarÅrskvantumKvote implements AksjonspunktOppdaterer<AvklarÅrskvantumDto> {

    private static final Logger log = LoggerFactory.getLogger(AvklarÅrskvantumKvote.class);

    ÅrskvantumTjeneste årskvantumTjeneste;

    AvklarÅrskvantumKvote() {
        // for CDI proxy
    }

    @Inject
    AvklarÅrskvantumKvote(ÅrskvantumTjeneste årskvantumTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarÅrskvantumDto dto, AksjonspunktOppdaterParameter param) {
        var fortsettBehandling = dto.getfortsettBehandling();

        // TODO: historikkinnslag

        log.info("Avklarer uttak årskvantum med status: " + fortsettBehandling + " og behandlingsId: " + param.getBehandlingId());

        if (fortsettBehandling) {
            //Bekreft uttaksplan og fortsett behandling
            årskvantumTjeneste.bekreftUttaksplan(param.getBehandlingId());
        }

        return OppdateringResultat.utenOveropp();

    }
}
