package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;

@ApplicationScoped
@ProsessTask(value = AvbrytEtterlysningTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class AvbrytEtterlysningTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "etterlysning.avbryt";
    private EtterlysningRepository etterlysningRepository;

    public AvbrytEtterlysningTask() {
        // CDI
    }

    public AvbrytEtterlysningTask(EtterlysningRepository etterlysningRepository) {
        this.etterlysningRepository = etterlysningRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        final var behandlingId = prosessTaskData.getBehandlingId();
        final var etterlysninger = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(Long.parseLong(behandlingId));
        // Kall oppgave API

        etterlysninger.forEach(Etterlysning::avbryt);
        etterlysningRepository.lagre(etterlysninger);
    }

}
