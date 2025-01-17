package no.nav.ung.domenetjenester.sak.innsending;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.domenetjenester.sak.UngSakSendInnJournalpostTask;
import no.nav.ung.fordel.repo.journalpost.JournalpostInnsendingEntitet;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;

@Dependent
public class SendInnJournalpostTjeneste {

    private final JournalpostRepository journalpostInnsendingRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;


    @Inject
    SendInnJournalpostTjeneste(JournalpostRepository journalpostInnsendingRepository,
                               ProsessTaskTjeneste prosessTaskTjeneste) {

        this.journalpostInnsendingRepository = journalpostInnsendingRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }


    public void klargjørUngSakInnsending(JournalpostInnsendingEntitet innsending) {
        journalpostInnsendingRepository.lagreInnsending(innsending);
        opprettTaskForUngSakInnsending(innsending);
    }

    private void opprettTaskForUngSakInnsending(JournalpostInnsendingEntitet innsending) {
        var data = new ProsessTaskData(UngSakSendInnJournalpostTask.class);
        håndterProsessTaskData(innsending, data);
    }

    private void håndterProsessTaskData(JournalpostInnsendingEntitet innsending, ProsessTaskData data) {
        data.setCallIdFraEksisterende();

        data.setSaksnummer(innsending.getSaksnummer().getVerdi());
        data.setProperty(UngSakSendInnJournalpostTask.PROPERTY_YTELSE_TYPE, innsending.getYtelseType().getKode());
        prosessTaskTjeneste.lagre(data);
    }
}
