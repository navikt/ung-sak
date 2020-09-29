package no.nav.k9.sak.behandling.prosessering.task;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * Kjører tilbakehopp til starten av beregning
 */
@ApplicationScoped
@ProsessTask(TilbakeTilStartBeregningTask.TASKNAME)
// gruppeSekvens = false for å kunne hoppe tilbake ved feilende fortsettBehandling task
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class TilbakeTilStartBeregningTask extends BehandlingProsessTask {

    public static final String TASKNAME = "beregning.tilbakeTilStart";

    private BehandlingRepository behandlingRepository;
    private HistorikkRepository historikkRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;
    private FagsakProsessTaskRepository prosessTaskRepository;

    TilbakeTilStartBeregningTask() {
        // for CDI proxy
    }

    @Inject
    public TilbakeTilStartBeregningTask(BehandlingRepository behandlingRepository,
                                        BehandlingLåsRepository behandlingLåsRepository,
                                        HistorikkRepository historikkRepository,
                                        ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                        BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                        FagsakProsessTaskRepository prosessTaskRepository) {
        super(behandlingLåsRepository);
        this.behandlingRepository = behandlingRepository;
        this.historikkRepository = historikkRepository;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData) {
        String behandlingId = prosessTaskData.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        logContext(behandling);
        
        if(!behandling.erSaksbehandlingAvsluttet() && behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandling.getId(), BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING)) {
            Long fagsakId = prosessTaskData.getFagsakId();
            BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
            prosessTaskRepository.settFeiletTilSuspendert(fagsakId, behandling.getId());
            hoppTilbake(behandling, BehandlingStegType.PRECONDITION_BEREGNING, kontekst);

        }
    }

    private void hoppTilbake(Behandling behandling, BehandlingStegType tilSteg, BehandlingskontrollKontekst kontekst) {
        doHoppTilSteg(behandling, kontekst, tilSteg);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        prosesseringAsynkTjeneste.asynkProsesserBehandlingMergeGruppe(behandling);
    }

    private void doHoppTilSteg(Behandling behandling, BehandlingskontrollKontekst kontekst, BehandlingStegType tilSteg) {
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        lagHistorikkinnslag(behandling, tilSteg.getNavn());

        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, tilSteg);
    }


    private void lagHistorikkinnslag(Behandling behandling, String tilStegNavn) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.SPOLT_TILBAKE);
        historikkinnslag.setBehandlingId(behandling.getId());

        String fraStegNavn = behandling.getAktivtBehandlingSteg() != null ? behandling.getAktivtBehandlingSteg().getNavn() : null;
        HistorikkInnslagTekstBuilder historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.SPOLT_TILBAKE)
            .medBegrunnelse("Behandlingen er flyttet fra " + fraStegNavn + " tilbake til " + tilStegNavn);
        historieBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

}
