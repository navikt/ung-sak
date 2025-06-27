package no.nav.ung.sak.behandling.revurdering.sats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.*;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.PERIODER;


/**
 * Batchtask som oppretter revurderinger på saker der brukere fyller 25 år.
 * <p>
 * Kjører hver dag kl 07:15.
 */
@ApplicationScoped
@ProsessTask(value = OpprettRevurderingHøySatsBatchTask.TASKNAME, cronExpression = "0 15 7 * * *", maxFailedRuns = 1)
public class OpprettRevurderingHøySatsBatchTask implements ProsessTaskHandler {

    public static final String TASKNAME = "batch.opprettRevurderingHøySats";

    private static final Logger log = LoggerFactory.getLogger(OpprettRevurderingHøySatsBatchTask.class);
    private SatsEndringRepository satsEndringRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;


    OpprettRevurderingHøySatsBatchTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettRevurderingHøySatsBatchTask(SatsEndringRepository satsEndringRepository, ProsessTaskTjeneste prosessTaskTjeneste, FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.satsEndringRepository = satsEndringRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LocalDate datoForKjøring = LocalDate.now();
        ProsessTaskGruppe taskGruppeTilRevurderinger = new ProsessTaskGruppe();

        Set<Map.Entry<Fagsak, LocalDate>> fagsakerTilRevurdering = satsEndringRepository.hentFagsakerMedBrukereSomFyller25ÅrFraDato(datoForKjøring).entrySet();


        List<ProsessTaskData> prosessTaskerTilRevurdering = utledProsessTaskerForRevurdering(fagsakerTilRevurdering);

        taskGruppeTilRevurderinger.addNesteParallell(prosessTaskerTilRevurdering);
        prosessTaskTjeneste.lagre(taskGruppeTilRevurderinger);
    }

    List<ProsessTaskData> utledProsessTaskerForRevurdering(Set<Map.Entry<Fagsak, LocalDate>> entries) {
        return entries
            .stream()
            .map(fagsakerTilVurdering -> {
                Fagsak fagsak = fagsakerTilVurdering.getKey();
                var prosesstaskerForFagsak = fagsakProsessTaskRepository.finnAlleForAngittSøk(fagsak.getId(), null, null, List.of(ProsessTaskStatus.KLAR, ProsessTaskStatus.VETO, ProsessTaskStatus.FEILET), true, null, null);
                if (prosesstaskerForFagsak.stream().anyMatch(task -> task.getTaskType().equals(OpprettRevurderingEllerOpprettDiffTask.TASKNAME))) {
                    log.info("Revurderingtask for fagsak med id {} eksisterer allerede, hopper over.", fagsak.getId());
                    return null; // Hopp over hvis vi allerede har en revurderingtask for denne fagsaken. Ellers går de i beina på hverandre.
                }
                LocalDate endringsdato = fagsakerTilVurdering.getValue();
                log.info("Oppretter revurdering for fagsak med id {} for økning av sats fra {}", fagsak.getId(), endringsdato);

                ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
                tilVurderingTask.setFagsakId(fagsak.getId());
                tilVurderingTask.setProperty(PERIODER, endringsdato + "/" + fagsak.getPeriode().getTomDato());
                tilVurderingTask.setProperty(BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS.getKode());
                return tilVurderingTask;
            }).filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
