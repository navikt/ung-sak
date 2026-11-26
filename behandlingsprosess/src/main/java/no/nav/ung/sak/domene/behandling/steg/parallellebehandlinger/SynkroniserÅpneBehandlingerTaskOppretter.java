package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;

/**
 * Oppretter {@link SynkroniserÅpneBehandlingerTask} for en fagsak.
 */
@Dependent
public class SynkroniserÅpneBehandlingerTaskOppretter {

    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    SynkroniserÅpneBehandlingerTaskOppretter() {
        // for CDI proxy
    }

    @Inject
    public SynkroniserÅpneBehandlingerTaskOppretter(ProsessTaskTjeneste prosessTaskTjeneste,
                                                    FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    /**
     * Oppretter synkronisering etter at en behandling er avsluttet. Tasken er tilstandsløs og finner selv siste vedtak,
     * så flere tasker på samme fagsak gir samme resultat.
     */
    public void opprett(Long fagsakId) {
        Objects.requireNonNull(fagsakId, "fagsakId");
        var task = ProsessTaskData.forProsessTask(SynkroniserÅpneBehandlingerTask.class);
        task.setFagsakId(fagsakId);
        task.setCallIdFraEksisterende();
        prosessTaskTjeneste.lagre(task);
    }

    /**
     * Oppretter synkronisering bare hvis fagsaken ikke allerede har en uferdig synkronisering. Brukes fra ventepunktet
     * for vedtaksprioritet, som kjøres på nytt ved hver gjenopptakelse.
     */
    public void opprettHvisIkkeFinnes(Long fagsakId) {
        if (!fagsakProsessTaskRepository.finnesUferdigTaskForFagsak(fagsakId, SynkroniserÅpneBehandlingerTask.TASKTYPE)) {
            opprett(fagsakId);
        }
    }
}
