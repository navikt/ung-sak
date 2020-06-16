package no.nav.k9.sak.økonomi.simulering.tjeneste;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.oppdrag.kontrakt.simulering.v1.SimuleringResultatDto;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.økonomi.simulering.SimulerOppdragAksjonspunktTjeneste;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;

@Dependent
public class SimulerInntrekkSjekkeTjeneste {

    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private HistorikkRepository historikkRepository;
    private Instance<SimulerOppdragAksjonspunktTjeneste> tjeneste;

    SimulerInntrekkSjekkeTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SimulerInntrekkSjekkeTjeneste(SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste,
                                         TilbakekrevingRepository tilbakekrevingRepository,
                                         HistorikkRepository historikkRepository,
                                         Instance<SimulerOppdragAksjonspunktTjeneste> tjeneste) {
        this.simuleringIntegrasjonTjeneste = simuleringIntegrasjonTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.historikkRepository = historikkRepository;
        this.tjeneste = tjeneste;
    }

    public void sjekkIntrekk(Behandling behandling) {
        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        if (tilbakekrevingValg.filter(valg -> valg.getVidereBehandling().equals(TilbakekrevingVidereBehandling.INNTREKK)).isPresent()) {
            simuleringIntegrasjonTjeneste.startSimulering(behandling);

            Optional<SimuleringResultatDto> simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling);
            if (simuleringResultatDto.isPresent()) {
                SimulerOppdragAksjonspunktTjeneste simulerOppdragAksjonspunktTjeneste = finnTjeneste(behandling);
                Optional<AksjonspunktDefinisjon> aksjonspunkt = simulerOppdragAksjonspunktTjeneste.utledAksjonspunkt(simuleringResultatDto.get());
                if (aksjonspunkt.filter(aksjonspunktDefinisjon -> aksjonspunktDefinisjon.equals(AksjonspunktDefinisjon.VURDER_FEILUTBETALING)).isPresent()) {
                    tilbakekrevingRepository.lagre(behandling, TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, null));
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
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_VIDERE_BEHANDLING, TilbakekrevingVidereBehandling.INNTREKK, TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING);
        tekstBuilder.build(innslag);
        historikkRepository.lagre(innslag);
    }

    private SimulerOppdragAksjonspunktTjeneste finnTjeneste(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(SimulerOppdragAksjonspunktTjeneste.class, tjeneste, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + SimulerOppdragAksjonspunktTjeneste.class.getSimpleName() + " for " + behandling.getUuid()));
    }

}
