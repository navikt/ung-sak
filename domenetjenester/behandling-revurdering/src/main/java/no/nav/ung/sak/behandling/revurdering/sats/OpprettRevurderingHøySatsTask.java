package no.nav.ung.sak.behandling.revurdering.sats;

import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.PERIODER;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
@ProsessTask(OpprettRevurderingHøySatsTask.TASKNAME)
public class OpprettRevurderingHøySatsTask implements ProsessTaskHandler {

    public static final String TASKNAME = "ung.opprettRevurderingHøySats";

    private static final Logger log = LoggerFactory.getLogger(OpprettRevurderingHøySatsTask.class);
    private SatsEndringRepository satsEndringRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;


    OpprettRevurderingHøySatsTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettRevurderingHøySatsTask(SatsEndringRepository satsEndringRepository, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.satsEndringRepository = satsEndringRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
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

    static List<ProsessTaskData> utledProsessTaskerForRevurdering(Set<Map.Entry<Fagsak, LocalDate>> entries) {
        return entries
            .stream()
            .map(fagsakerTilVurdering -> {
                Fagsak fagsak = fagsakerTilVurdering.getKey();
                LocalDate endringsdato = fagsakerTilVurdering.getValue();
                log.info("Oppretter revurdering for fagsak med id {}", fagsak.getId());

                ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
                tilVurderingTask.setFagsakId(fagsak.getId());
                tilVurderingTask.setProperty(PERIODER, endringsdato + "/" + fagsak.getPeriode().getTomDato());
                tilVurderingTask.setProperty(BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS.getKode());
                return tilVurderingTask;
            }).collect(Collectors.toList());
    }
}
