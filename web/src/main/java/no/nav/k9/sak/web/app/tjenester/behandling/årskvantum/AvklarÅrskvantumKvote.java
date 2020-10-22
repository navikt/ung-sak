package no.nav.k9.sak.web.app.tjenester.behandling.årskvantum;

import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
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
        Long behandlingId = param.getBehandlingId();

        if (fortsettBehandling) {
            //Bekreft uttaksplan og fortsett behandling
            opprettHistorikkInnslag(dto, behandlingId, HistorikkinnslagType.FASTSATT_UTTAK, "Fortsett uten endring, avslåtte perioder er korrekt");
            årskvantumTjeneste.bekreftUttaksplan(behandlingId);
        } else {
            // kjør steget på nytt, aka hent nye rammevedtak fra infotrygd
            opprettHistorikkInnslag(dto, behandlingId, HistorikkinnslagType.FAKTA_ENDRET, "Rammemelding er endret eller lagt til");
        }

        return OppdateringResultat.utenOveropp();
    }

    private void opprettHistorikkInnslag(AvklarÅrskvantumDto dto, Long behandlingId, HistorikkinnslagType historikkinnslagType, String valg) {
        HistorikkInnslagTekstBuilder builder = historikkTjenesteAdapter.tekstBuilder();
        builder.medEndretFelt(HistorikkEndretFeltType.VALG, null, valg);
        builder.medBegrunnelse(dto.getBegrunnelse());
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, historikkinnslagType);
    }
}
