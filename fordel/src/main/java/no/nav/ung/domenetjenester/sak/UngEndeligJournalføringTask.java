package no.nav.ung.domenetjenester.sak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.ung.domenetjenester.arkiv.dok.DokTjeneste;
import no.nav.ung.domenetjenester.arkiv.journal.TilJournalføringTjeneste;
import no.nav.ung.fordel.handler.FordelProsessTaskTjeneste;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;

@ApplicationScoped
@ProsessTask(value = UngEndeligJournalføringTask.TASKTYPE, maxFailedRuns = 1)
public class UngEndeligJournalføringTask extends EndeligJournalføringTask {

    public static final String TASKTYPE = "ung.endeligJournalføring";
    private static final Logger log = LoggerFactory.getLogger(UngEndeligJournalføringTask.class);

    @Inject
    public UngEndeligJournalføringTask(FordelProsessTaskTjeneste fordelProsessTaskTjeneste,
                                       TilJournalføringTjeneste journalTjeneste,
                                       JournalpostRepository journalpostRepository,
                                       DokTjeneste dokTjeneste) {
        super(fordelProsessTaskTjeneste, journalTjeneste, journalpostRepository);
    }

    @Override
    protected String endeligJouralføringTask() {
        return TASKTYPE;
    }

    @Override
    protected String sendtInnDokumentTask() {
        return UngSendInnDokumentTask.TASKTYPE;
    }
}
