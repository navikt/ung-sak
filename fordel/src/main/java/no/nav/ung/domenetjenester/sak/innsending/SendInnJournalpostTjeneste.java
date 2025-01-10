package no.nav.ung.domenetjenester.sak.innsending;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.domenetjenester.sak.UngSakSendInnJournalpostTask;
import no.nav.ung.fordel.repo.journalpost.JournalpostInnsendingEntitet;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;

@Dependent
public class SendInnJournalpostTjeneste {

    private final JournalpostRepository journalpostInnsendingRepository;
    private final ProsessTaskTjeneste prosessTaskTjeneste;

    /**
     * et utsettelsevindu gjør at vi venter for å se om flere meldinger av samme type / på samme sak er klare før vi sender inn.
     * F.eks. når det kommer flere inntektsmeldinger separat fra en arbeidsgiver, så tillater dette oss å vente til flere/alle er klare. Hvis
     * tiden løper ut faller logikken tilbake på at det kan behandles separat i fagsystemet.
     */
    private Duration utsettelseVindu;

    @Inject
    SendInnJournalpostTjeneste(JournalpostRepository journalpostInnsendingRepository,
                               ProsessTaskTjeneste prosessTaskTjeneste,
                               @KonfigVerdi(value = "K9_INNSENDING_VENTING_VINDU", required = false) String utsettelse) {

        if (utsettelse != null && !utsettelse.isBlank()) {
            this.utsettelseVindu = Duration.parse(utsettelse);
        }
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

        if (utsettelseVindu != null) {
            data.setNesteKjøringEtter(LocalDateTime.now().plus(utsettelseVindu));
        }

        data.setSaksnummer(innsending.getSaksnummer().getVerdi());
        data.setProperty(UngSakSendInnJournalpostTask.PROPERTY_YTELSE_TYPE, innsending.getYtelseType().getKode());
        prosessTaskTjeneste.lagre(data);
    }
}
