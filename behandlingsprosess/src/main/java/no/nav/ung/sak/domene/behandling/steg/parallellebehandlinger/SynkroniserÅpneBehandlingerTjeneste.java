package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallellBehandling;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;

/**
 * Oppdaterer åpne ytelsesbehandlinger med vedtatte data og tar dem av vent. Se {@link SynkroniserÅpneBehandlingerTask}.
 */
@Dependent
public class SynkroniserÅpneBehandlingerTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SynkroniserÅpneBehandlingerTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;
    private ParallelleBehandlingerTjeneste parallelleBehandlingerTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    SynkroniserÅpneBehandlingerTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SynkroniserÅpneBehandlingerTjeneste(BehandlingRepository behandlingRepository,
                                               ParallelleBehandlingerRepository parallelleBehandlingerRepository,
                                               ParallelleBehandlingerTjeneste parallelleBehandlingerTjeneste,
                                               BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.parallelleBehandlingerRepository = parallelleBehandlingerRepository;
        this.parallelleBehandlingerTjeneste = parallelleBehandlingerTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    public void synkroniser(ProsessTaskData prosessTaskData) {
        Long fagsakId = prosessTaskData.getFagsakId();
        Optional<Long> kilde = parallelleBehandlingerRepository.finnSisteAvsluttedeIkkeHenlagteYtelsesbehandlingId(fagsakId);
        for (ParallellBehandling åpen : parallelleBehandlingerRepository.hentIkkeAvsluttedeYtelsesbehandlinger(fagsakId)) {
            Behandling behandling = behandlingRepository.hentBehandling(åpen.behandlingId());
            if (parallelleBehandlingerTjeneste.byggerPåSisteVedtak(fagsakId, åpen.behandlingId())) {
                vekkHvisVenterIPorten(behandling);
            } else {
                throw new IllegalStateException("Synkronisering av data fra behandling " + kilde.orElse(null) + " til " + åpen.behandlingId() + " er ikke implementert");
            }
        }
    }

    private void vekkHvisVenterIPorten(Behandling behandling) {
        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_PARALLELL_BEHANDLING)) {
            log.info("Gjenopptar behandling {} som venter i porten", behandling.getId());
            behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, false);
        }
    }
}
