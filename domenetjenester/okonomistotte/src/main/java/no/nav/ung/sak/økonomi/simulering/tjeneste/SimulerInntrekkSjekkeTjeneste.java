package no.nav.ung.sak.økonomi.simulering.tjeneste;

import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;

@Dependent
public class SimulerInntrekkSjekkeTjeneste {

    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private HistorikkRepository historikkRepository;

    SimulerInntrekkSjekkeTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SimulerInntrekkSjekkeTjeneste(SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste,
                                         TilbakekrevingRepository tilbakekrevingRepository,
                                         HistorikkRepository historikkRepository) {
        this.simuleringIntegrasjonTjeneste = simuleringIntegrasjonTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.historikkRepository = historikkRepository;
    }

    /**
     * denne tjenesten brukes når vedtak fattes. Hvis det i simulering-steget ikke ble identifiseret feilutbetaling,
     * men det identifiseres på dette tidspunktet, blir tidligere valg angående tilbakekreving reversert da de nå er ugyldige,
     * og automatisk satt til at det skal opprettes tilbakekrevingsbehandling
     */
    public void sjekkIntrekk(Behandling behandling) {
        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        if (tilbakekrevingValg.filter(valg -> valg.getVidereBehandling().equals(TilbakekrevingVidereBehandling.INNTREKK)).isPresent()) {
            simuleringIntegrasjonTjeneste.startSimulering(behandling);

            Optional<SimuleringResultatDto> simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling);
            if (simuleringResultatDto.isPresent() && simuleringResultatDto.get().harFeilutbetaling()) {
                tilbakekrevingRepository.lagre(behandling, TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, null));
                opprettHistorikkInnslag(behandling.getId());
            }
        }
    }

    private void opprettHistorikkInnslag(Long behandlingId) {
        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        innslag.setBehandlingId(behandlingId);
        innslag.setType(HistorikkinnslagType.TILBAKEKREVING_VIDEREBEHANDLING);

        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder().medSkjermlenke(SkjermlenkeType.FAKTA_OM_SIMULERING);
        tekstBuilder.medHendelse(innslag.getType());
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_VIDERE_BEHANDLING, TilbakekrevingVidereBehandling.INNTREKK, TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING);
        tekstBuilder.build(innslag);
        historikkRepository.lagre(innslag);
    }
}
