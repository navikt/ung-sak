package no.nav.ung.sak.økonomi.task;

import static no.nav.ung.sak.økonomi.task.VurderOppgaveTilbakekrevingTask.TASKTYPE;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingRepository;
import no.nav.ung.sak.økonomi.tilbakekreving.modell.TilbakekrevingValg;

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
        logContext(behandling);

        var ref = BehandlingReferanse.fra(behandling);
        if (ref.getFagsakYtelseType() == FagsakYtelseType.FRISINN && skalOppretteOppgaveTilbakekreving(behandling)) {
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
