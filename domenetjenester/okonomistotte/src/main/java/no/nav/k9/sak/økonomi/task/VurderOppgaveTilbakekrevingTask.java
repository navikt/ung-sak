package no.nav.k9.sak.økonomi.task;

import static no.nav.k9.sak.økonomi.task.VurderOppgaveTilbakekrevingTask.TASKTYPE;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.k9.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOppgaveTilbakekrevingTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "oppgavebehandling.vurderOppgaveTilbakekreving";
    private static final Logger log = LoggerFactory.getLogger(VurderOppgaveTilbakekrevingTask.class);
    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingRepository behandlingRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;

    VurderOppgaveTilbakekrevingTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOppgaveTilbakekrevingTask(OppgaveTjeneste oppgaveTjeneste, BehandlingRepositoryProvider repositoryProvider,
                                           TilbakekrevingRepository tilbakekrevingRepository) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        if (skalOppretteOppgaveTilbakekreving(behandling)) {
            FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
            String beskrivelse = "Feilutbetaling " + fagsakYtelseType + ". Opprett en tilbakekrevingsbehandling i Infotrygd.";

            String oppgaveId = oppgaveTjeneste.opprettOppgaveFeilutbetaling(ref, beskrivelse);
            log.info("Opprettet oppgave i GSAK for tilbakebetaling. BehandlingId: {}. OppgaveId: {}.", behandlingId, oppgaveId);
        }
    }

    private boolean skalOppretteOppgaveTilbakekreving(Behandling behandling) {
        Optional<TilbakekrevingValg> funnetTilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        return funnetTilbakekrevingValg.map(tilbakekrevingValg ->
            TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING.equals(tilbakekrevingValg.getVidereBehandling()))
            .orElse(false);
    }

}
