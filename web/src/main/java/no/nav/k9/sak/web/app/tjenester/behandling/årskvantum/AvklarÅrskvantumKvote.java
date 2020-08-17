package no.nav.k9.sak.web.app.tjenester.behandling.årskvantum;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.kontrakt.uttak.AvklarÅrskvantumDto;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarÅrskvantumDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarÅrskvantumKvote implements AksjonspunktOppdaterer<AvklarÅrskvantumDto> {

    private HistorikkRepository historikkRepository;

    ÅrskvantumTjeneste årskvantumTjeneste;

    AvklarÅrskvantumKvote() {
        // for CDI proxy
    }

    @Inject
    AvklarÅrskvantumKvote(HistorikkRepository historikkRepository,
                          ÅrskvantumTjeneste årskvantumTjeneste) {
        this.historikkRepository = historikkRepository;
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarÅrskvantumDto dto, AksjonspunktOppdaterParameter param) {
        var fortsettBehandling = dto.getfortsettBehandling();

        if (fortsettBehandling) {
            Long behandlingId = param.getBehandlingId();
            Long fagsakId = param.getRef().getFagsakId();
            HistorikkinnslagType type = HistorikkinnslagType.OVST_UTTAK;
            Venteårsak venteårsak = Venteårsak.PERIODE_MED_AVSLAG;
            opprettHistorikkinnslag(behandlingId, fagsakId, type, venteårsak);

            //Bekreft uttaksplan og fortsett behandling

            årskvantumTjeneste.bekreftUttaksplan(behandlingId);
        }

        return OppdateringResultat.utenOveropp();
    }

    private void opprettHistorikkinnslag(Long behandlingId,
                                         Long fagsakId,
                                         HistorikkinnslagType historikkinnslagType,
                                         Venteårsak venteårsak) {
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder();

        builder.medHendelse(historikkinnslagType);

        if (venteårsak != null) {
            builder.medÅrsak(venteårsak);
        }
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setType(historikkinnslagType);
        historikkinnslag.setBehandlingId(behandlingId);
        historikkinnslag.setFagsakId(fagsakId);
        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }
}
