package no.nav.k9.sak.web.app.tjenester.behandling.årskvantum;

import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.uttak.AvklarÅrskvantumDto;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarÅrskvantumDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarÅrskvantumKvote implements AksjonspunktOppdaterer<AvklarÅrskvantumDto> {

    HistorikkTjenesteAdapter historikkTjenesteAdapter;

    ÅrskvantumTjeneste årskvantumTjeneste;

    AvklarÅrskvantumKvote() {
        // for CDI proxy
    }

    @Inject
    AvklarÅrskvantumKvote(HistorikkTjenesteAdapter historikkTjenesteAdapter,
                          ÅrskvantumTjeneste årskvantumTjeneste) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarÅrskvantumDto dto, AksjonspunktOppdaterParameter param) {
        var fortsettBehandling = dto.getfortsettBehandling();

        if (fortsettBehandling) {
            Long behandlingId = param.getBehandlingId();

            historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.OVST_UTTAK);

            //Bekreft uttaksplan og fortsett behandling

            årskvantumTjeneste.bekreftUttaksplan(behandlingId);
        }

        return OppdateringResultat.utenOveropp();
    }
}
