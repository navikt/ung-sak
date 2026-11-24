package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;

/**
 * Oppretter {@link SynkroniserÅpneBehandlingerTask} når en ytelsesbehandling avsluttes og fagsaken har andre åpne
 * ytelsesbehandlinger. Gjelder også henleggelse, slik at behandlinger som venter i porten, tas av vent.
 * <p>
 * Tasken opprettes i samme transaksjon som avslutningen. Den lages bare når bryteren for ytelsen er på, eller når
 * fagsaken har en åpen kontrollbehandling. Da blir åpne kontrollbehandlinger fullført også etter at bryteren er slått av.
 */
@ApplicationScoped
public class OpprettSynkVedAvslutningObserver {

    private static final Logger log = LoggerFactory.getLogger(OpprettSynkVedAvslutningObserver.class);

    private BehandlingRepository behandlingRepository;
    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;
    private SynkroniserÅpneBehandlingerTaskOppretter synkroniserÅpneBehandlingerTaskOppretter;
    private KontrollbehandlingBryter kontrollbehandlingBryter;

    OpprettSynkVedAvslutningObserver() {
        // for CDI proxy
    }

    @Inject
    public OpprettSynkVedAvslutningObserver(BehandlingRepository behandlingRepository,
                                            ParallelleBehandlingerRepository parallelleBehandlingerRepository,
                                            SynkroniserÅpneBehandlingerTaskOppretter synkroniserÅpneBehandlingerTaskOppretter,
                                            KontrollbehandlingBryter kontrollbehandlingBryter) {
        this.behandlingRepository = behandlingRepository;
        this.parallelleBehandlingerRepository = parallelleBehandlingerRepository;
        this.synkroniserÅpneBehandlingerTaskOppretter = synkroniserÅpneBehandlingerTaskOppretter;
        this.kontrollbehandlingBryter = kontrollbehandlingBryter;
    }

    public void vedAvslutning(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        Behandling behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        if (!behandling.erYtelseBehandling()) {
            return;
        }
        Long fagsakId = behandling.getFagsakId();
        if (parallelleBehandlingerRepository.hentAndreIkkeAvsluttedeYtelsesbehandlinger(fagsakId, behandling.getId()).isEmpty()) {
            return;
        }
        if (kontrollbehandlingBryter.erPå(behandling.getFagsakYtelseType())
            || parallelleBehandlingerRepository.finnIkkeAvsluttetKontrollbehandling(fagsakId).isPresent()) {
            log.info("Behandling {} er avsluttet. Oppretter synkronisering av åpne behandlinger på fagsaken", behandling.getId());
            synkroniserÅpneBehandlingerTaskOppretter.opprett(fagsakId);
        }
    }
}
