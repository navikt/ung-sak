package no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering;

import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;

@ApplicationScoped
@ProsessTask(TmpAktørIdTask.TASKTYPE)
public class TmpAktørIdTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "rapportering.identCache";

    private static final Logger log = LoggerFactory.getLogger(TmpAktørIdTask.class);
    private TmpAktoerIdRepository aktørIdRepository;
    private AktørTjeneste aktørTjeneste;
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    public TmpAktørIdTask(TmpAktoerIdRepository aktørIdRepository,
                          ProsessTaskRepository prosessTaskRepository,
                          AktørTjeneste aktørTjeneste) {
        this.aktørIdRepository = aktørIdRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.aktørTjeneste = aktørTjeneste;
    }

    TmpAktørIdTask() {
        //
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        int max = 100;
        var aktørIder = new TreeSet<>(aktørIdRepository.finnManglendeMapping(max));

        if (aktørIder.isEmpty()) {
            log.info("Ingen flere aktørId å hente inn per nå, avslutter innhenting");
        } else {
            log.info("Fant {} aktørId å hente ident for, spør PDL", aktørIder.size());
            var mapIdenter = aktørTjeneste.hentPersonIdentForAktørIder(aktørIder);
            log.info("Hentet {} identer", mapIdenter.size());
            aktørIdRepository.lagre(mapIdenter);

            // rescheduler
            var prosessTaskDataNy = new ProsessTaskData(TASKTYPE);
            prosessTaskDataNy.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(prosessTaskDataNy);
        }
    }

}
