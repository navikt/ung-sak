package no.nav.k9.sak.behandling.revurdering;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;

/**
 * Kjører tilbakehopp til starten av prosessen. Brukes til rekjøring av saker som må gjøre alt på nytt.
 */
@ApplicationScoped
@ProsessTask(OpprettRevurderingEllerOpprettDiffTask.TASKNAME)
// gruppeSekvens = false for å kunne hoppe tilbake ved feilende fortsettBehandling task
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class OpprettRevurderingEllerOpprettDiffTask extends FagsakProsessTask {

    public static final String TASKNAME = "behandlingskontroll.opprettRevurderingEllerDiff";
    public static final String BEHANDLING_ÅRSAK = "behandlingArsak";
    private static final Logger log = LoggerFactory.getLogger(OpprettRevurderingEllerOpprettDiffTask.class);
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    OpprettRevurderingEllerOpprettDiffTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettRevurderingEllerOpprettDiffTask(FagsakRepository fagsakRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  BehandlingLåsRepository behandlingLåsRepository,
                                                  FagsakLåsRepository fagsakLåsRepository,
                                                  BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste,
                                                  BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        super(fagsakLåsRepository, behandlingLåsRepository);
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var fagsakId = prosessTaskData.getFagsakId();
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        logContext(fagsak);

        var behandlinger = behandlingRepository.hentÅpneBehandlingerIdForFagsakId(fagsakId);
        final BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.fraKode(prosessTaskData.getPropertyValue(BEHANDLING_ÅRSAK));
        if (behandlinger.isEmpty()) {
            var sisteVedtak = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);

            final RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
            if (sisteVedtak.isPresent() && revurderingTjeneste.kanRevurderingOpprettes(fagsak)) {
                var origBehandling = sisteVedtak.get();
                var behandling = revurderingTjeneste.opprettAutomatiskRevurdering(origBehandling, behandlingÅrsakType, origBehandling.getBehandlendeOrganisasjonsEnhet());
                log.info("Oppretter revurdering='{}' basert på '{}'", behandling, origBehandling);
                behandlingsprosessApplikasjonTjeneste.asynkStartBehandlingsprosess(behandling);
            } else {
                throw new IllegalStateException("Prøvde revurdering av sak, men fant ingen behandling revurdere");
            }
        } else {
            if (behandlinger.size() != 1) {
                throw new IllegalStateException("Fant flere åpne behandlinger");
            }
            var behandlingId = behandlinger.get(0);
            log.info("Fant åpen behandling='{}', kjører diff for å flytte prosessen tilbake", behandlingId);
            var behandlingLås = behandlingRepository.taSkriveLås(behandlingId);
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            BehandlingÅrsak.builder(behandlingÅrsakType).buildFor(behandling);
            behandlingRepository.lagre(behandling, behandlingLås);
            behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, false);
        }
    }

}
