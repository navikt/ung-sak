package no.nav.foreldrepenger.økonomi.simulering.tjeneste;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.økonomi.simulering.SimulerOppdragAksjonspunktUtleder;
import no.nav.foreldrepenger.økonomi.simulering.SimulerOppdragApplikasjonTjeneste;
import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimuleringResultatDto;

@ApplicationScoped
public class SimulerInntrekkSjekkeTjeneste {

    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private SimulerOppdragApplikasjonTjeneste simulerOppdragTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private HistorikkRepository historikkRepository;

    private static final long DUMMY_TASK_ID = -1L;

    SimulerInntrekkSjekkeTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SimulerInntrekkSjekkeTjeneste(SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste,
                                         SimulerOppdragApplikasjonTjeneste simulerOppdragTjeneste,
                                         TilbakekrevingRepository tilbakekrevingRepository,
                                         HistorikkRepository historikkRepository) {
        this.simuleringIntegrasjonTjeneste = simuleringIntegrasjonTjeneste;
        this.simulerOppdragTjeneste = simulerOppdragTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.historikkRepository = historikkRepository;
    }

    public void sjekkIntrekk(Behandling behandling) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return;
        }
        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        if (tilbakekrevingValg.filter(valg -> valg.getVidereBehandling().equals(TilbakekrevingVidereBehandling.INNTREKK)).isPresent()) {
            List<String> oppdragXmler = simulerOppdragTjeneste.simulerOppdrag(behandling.getId(), DUMMY_TASK_ID);
            simuleringIntegrasjonTjeneste.startSimulering(behandling.getId(), oppdragXmler);

            Optional<SimuleringResultatDto> simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling.getId());
            if (simuleringResultatDto.isPresent()) {
                Optional<AksjonspunktDefinisjon> aksjonspunkt = SimulerOppdragAksjonspunktUtleder.utledAksjonspunkt(simuleringResultatDto.get());
                if (aksjonspunkt.filter(aksjonspunktDefinisjon -> aksjonspunktDefinisjon.equals(AksjonspunktDefinisjon.VURDER_FEILUTBETALING)).isPresent()) {
                    tilbakekrevingRepository.lagre(behandling, TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD, null));
                    opprettHistorikkInnslag(behandling.getId());
                }
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
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_VIDERE_BEHANDLING, TilbakekrevingVidereBehandling.INNTREKK, TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD);
        tekstBuilder.build(innslag);
        historikkRepository.lagre(innslag);
    }

}
