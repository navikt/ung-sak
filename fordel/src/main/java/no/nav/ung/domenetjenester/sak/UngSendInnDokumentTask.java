package no.nav.ung.domenetjenester.sak;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.ung.domenetjenester.sak.innsending.SendInnJournalpostTjeneste;
import no.nav.ung.fordel.handler.FordelProsessTaskTjeneste;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.fordel.handler.WrappedProsessTaskHandler;
import no.nav.ung.fordel.repo.journalpost.JournalpostInnsendingEntitet;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;

@ApplicationScoped
@ProsessTask(value = UngSendInnDokumentTask.TASKTYPE, maxFailedRuns = 1)
public class UngSendInnDokumentTask extends WrappedProsessTaskHandler {

    public static final String TASKTYPE = "ung.dokumentInnsendelse";

    private static final Logger log = LoggerFactory.getLogger(UngSendInnDokumentTask.class);
    private final SendInnJournalpostTjeneste sendInnJournalpostTjeneste;
    private final JournalpostRepository journalpostRepository;

    @Inject
    public UngSendInnDokumentTask(FordelProsessTaskTjeneste fordelProsessTaskTjeneste,
                                  SendInnJournalpostTjeneste sendInnJournalpostTjeneste,
                                  JournalpostRepository journalpostRepository) {
        super(fordelProsessTaskTjeneste);
        this.sendInnJournalpostTjeneste = sendInnJournalpostTjeneste;
        this.journalpostRepository = journalpostRepository;
    }

    @Override
    public void precondition(MottattMelding dataWrapper) {
        if (dataWrapper.getSaksnummer().isEmpty()) {
            throw new IllegalStateException("Mangler påkrevd saksnummer.");
        }
        if (dataWrapper.getPayloadAsString().isEmpty()) {
            throw new IllegalStateException("Mangler påkrevd payload.");
        }
        var ytelseType = dataWrapper.getYtelseType();
        var manglerYtelseType = ytelseType.isEmpty();
        if (dataWrapper.getBehandlingTema() == null && manglerYtelseType) {
            throw new IllegalStateException("Mangler påkrevd behandlingTema eller ytelseType.");
        }
        if (dataWrapper.getBrevkode() == null) {
            throw new IllegalStateException("Mangler påkrevd brevkode.");
        }
    }

    @Override
    public MottattMelding doTask(MottattMelding dataWrapper) {
        final var saksnummer = new Saksnummer(dataWrapper.getSaksnummer().orElseThrow());
        final var journalPostId0 = dataWrapper.getJournalPostId();
        final var journalpostId = new JournalpostId(journalPostId0.getVerdi());
        final String payload = dataWrapper.getPayloadAsString().orElseThrow();

        var ytelseType = getYtelseType(dataWrapper, saksnummer, journalpostId);
        doSendTilK9JournalpostAnkommet(dataWrapper, saksnummer, payload, ytelseType);

        journalpostRepository.markerJournalposterBehandlet(journalPostId0);
        return null;
    }

    private void doSendTilK9JournalpostAnkommet(MottattMelding dataWrapper, Saksnummer saksnummer, String payload, FagsakYtelseType ytelseType) {
        sendInnJournalpostIEgenTask(dataWrapper, saksnummer, ytelseType, payload);
    }

    private void sendInnJournalpostIEgenTask(MottattMelding dataWrapper, Saksnummer saksnummer, FagsakYtelseType ytelseType, String payload) {
        JournalpostInnsendingEntitet.Status status = JournalpostInnsendingEntitet.Status.UBEHANDLET;

        var brevkode = Optional.ofNullable(Brevkode.fraKode(dataWrapper.getBrevkode()))
                .orElseThrow(() -> new IllegalArgumentException("Mangler mapping for brevkode: " + dataWrapper.getBrevkode()));
        var journalPostId = new JournalpostId(dataWrapper.getJournalPostId().getVerdi());
        var innsendingstidspunkt = dataWrapper.getForsendelseMottattTidspunkt().orElseThrow();
        AktørId aktørId = dataWrapper.getAktørId().map(AktørId::new)
                .orElseThrow(() -> new IllegalStateException("Mangler aktørid for journalpost: " + journalPostId));

        var journalpostInnsending = new JournalpostInnsendingEntitet(
                ytelseType,
                new Saksnummer(saksnummer.getVerdi()),
                new JournalpostId(journalPostId.getVerdi()),
                new AktørId(aktørId.getId()),
                brevkode,
            innsendingstidspunkt,
                payload,
                status
        );

        sendInnJournalpostTjeneste.klargjørUngSakInnsending(journalpostInnsending);
    }

    private FagsakYtelseType getYtelseType(MottattMelding dataWrapper, Saksnummer saksnummer, JournalpostId journalpostId) {
        return dataWrapper.getYtelseType()
                .orElseThrow(() -> new IllegalStateException("Mangler ytelseType for saksnummer=" + saksnummer + ", journalpostId=" + journalpostId));
    }

}
