package no.nav.k9.sak.økonomi.tilbakekreving.samkjøring;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.økonomi.tilbakekreving.klient.K9TilbakeRestKlient;

@ApplicationScoped
@ProsessTask(value = GjenopptaVenterPåTilbakekrevingTask.TASKTYPE)
public class GjenopptaVenterPåTilbakekrevingTask extends BehandlingProsessTask {

    private static final Logger logger = LoggerFactory.getLogger(GjenopptaVenterPåTilbakekrevingTask.class);

    public static final String TASKTYPE = "gjenopptaVenterPåTilbakekrevingTask";

    private BehandlingLåsRepository behandlingLåsRepository;
    private BehandlingRepository behandlingRepository;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;
    private K9TilbakeRestKlient k9TilbakeRestKlient;
    private SjekkTilbakekrevingAksjonspunktUtleder sjekkTilbakekrevingAksjonspunktUtleder;

    public GjenopptaVenterPåTilbakekrevingTask() {
        //for CDI proxy
    }

    @Inject
    public GjenopptaVenterPåTilbakekrevingTask(BehandlingLåsRepository behandlingLåsRepository,
                                               BehandlingRepository behandlingRepository,
                                               HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                               BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                               BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste,
                                               K9TilbakeRestKlient k9TilbakeRestKlient,
                                               SjekkTilbakekrevingAksjonspunktUtleder sjekkTilbakekrevingAksjonspunktUtleder) {
        super(behandlingLåsRepository);
        this.behandlingLåsRepository = behandlingLåsRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.k9TilbakeRestKlient = k9TilbakeRestKlient;
        this.sjekkTilbakekrevingAksjonspunktUtleder = sjekkTilbakekrevingAksjonspunktUtleder;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        BehandlingLås behandlingLås = behandlingLåsRepository.taLås(prosessTaskData.getBehandlingId());
        Behandling behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());

        if (sjekkTilbakekrevingAksjonspunktUtleder.påvirkerÅpenTilbakekrevingsbehandling(behandling)) {
            logger.info("Gjør ikke noe med behandlingen, siden det finnes en åpen tilbakekreving som vil bli påvirket av denne.");
            return;
        }
        Optional<Aksjonspunkt> autopunkt = finnAutopunktVenterPåTilbakekreving(behandling);
        if (autopunkt.isPresent()) {
            Aksjonspunkt autopunktet = autopunkt.get();
            autopunktet.avbryt();
            logger.info("Avbryter autopunkt {} med venteårsak {}", autopunktet.getAksjonspunktDefinisjon(), autopunktet.getVenteårsak());
            historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.BEH_GJEN, HistorikkAktør.VEDTAKSLØSNINGEN);
        }
        Optional<Aksjonspunkt> aksjonspunkt = finnSjekkTilbakekrevingAksjonspunkt(behandling);
        if (aksjonspunkt.isPresent()) {
            logger.info("Avbryter aksjonspunkt {}", aksjonspunkt.get().getAksjonspunktDefinisjon());
            aksjonspunkt.get().avbryt();
        }

        if (aksjonspunkt.isPresent() || autopunkt.isPresent()) {
            behandlingRepository.lagre(behandling, behandlingLås);

            if (!k9TilbakeRestKlient.harÅpenTilbakekrevingsbehandling(behandling.getFagsak().getSaksnummer())) {
                logger.info("Tilbakekrevingsbehandlingen er ferdig, kjører simulering-steget på nytt");
                BehandlingskontrollKontekst behandlingskontrollKontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
                behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(behandlingskontrollKontekst, BehandlingStegType.SIMULER_OPPDRAG);
            }
            if (prosessKanFortsette(behandling)) {
                behandlingsprosessApplikasjonTjeneste.asynkRegisteroppdateringKjørProsess(behandling);
            }
        }

    }

    private Optional<Aksjonspunkt> finnSjekkTilbakekrevingAksjonspunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.SJEKK_TILBAKEKREVING);
    }

    private Optional<Aksjonspunkt> finnAutopunktVenterPåTilbakekreving(Behandling behandling) {
        return behandling.getAksjonspunkter().stream()
            .filter(a -> a.getAksjonspunktDefinisjon().erAutopunkt())
            .filter(a -> a.erÅpentAksjonspunkt())
            .filter(a -> a.getVenteårsak() == Venteårsak.VENT_TILBAKEKREVING)
            .findFirst();
    }

    boolean prosessKanFortsette(Behandling behandling) {
        if (behandling.isBehandlingPåVent()) {
            logger.info("Fortsetter ikke behandling. Behandlingen er på vent.");
            return false;
        }
        if (harÅpneAksjonspunkterISteget(behandling)) {
            logger.info("Fortsetter ikke behandling. Behandlingen har åpne aksjonspunkter i inneværende steg.");
            return false;
        }
        return true;
    }


    private boolean harÅpneAksjonspunkterISteget(Behandling behandling) {
        return behandling.getAksjonspunkter().stream().anyMatch(a -> a.getAksjonspunktDefinisjon().getBehandlingSteg().equals(behandling.getAktivtBehandlingSteg()) && a.getStatus().erÅpentAksjonspunkt());
    }
}
