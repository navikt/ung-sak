package no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger;

import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.uttak.AvklarÅrskvantumDokDto;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarÅrskvantumDokDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarÅrskvantumDok implements AksjonspunktOppdaterer<AvklarÅrskvantumDokDto> {

    HistorikkTjenesteAdapter historikkTjenesteAdapter;

    ÅrskvantumTjeneste årskvantumTjeneste;

    AvklarÅrskvantumDok() {
        // for CDI proxy
    }

    @Inject
    AvklarÅrskvantumDok(HistorikkTjenesteAdapter historikkTjenesteAdapter,
                          ÅrskvantumTjeneste årskvantumTjeneste) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarÅrskvantumDokDto dto, AksjonspunktOppdaterParameter param) {
        var fortsettBehandling = dto.getfortsettBehandling();
        var innvilgePeriodene = dto.getinnvilgePeriodene();
        Long behandlingId = param.getBehandlingId();
        var manuellVurderingString = innvilgePeriodene ? "innvilget" : "avslått";

        if (fortsettBehandling) {
            //Bekreft uttaksplan og fortsett behandling
            opprettHistorikkInnslag(dto, behandlingId, HistorikkinnslagType.FASTSATT_UTTAK, "Uavklarte perioder er " + manuellVurderingString);
            årskvantumTjeneste.innvilgeEllerAvslåPeriodeneManuelt(behandlingId, innvilgePeriodene);
        }

        return OppdateringResultat.utenOveropp();
    }

    private void opprettHistorikkInnslag(AvklarÅrskvantumDokDto dto, Long behandlingId, HistorikkinnslagType historikkinnslagType, String valg) {
        HistorikkInnslagTekstBuilder builder = historikkTjenesteAdapter.tekstBuilder();
        builder.medEndretFelt(HistorikkEndretFeltType.VALG, null, valg);
        builder.medBegrunnelse(dto.getBegrunnelse());
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, historikkinnslagType);
    }

}
