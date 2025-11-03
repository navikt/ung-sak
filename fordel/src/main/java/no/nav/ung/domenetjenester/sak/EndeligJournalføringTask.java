package no.nav.ung.domenetjenester.sak;

import no.nav.ung.domenetjenester.arkiv.dok.DokarkivException;
import no.nav.ung.domenetjenester.arkiv.journal.TilJournalføringTjeneste;
import no.nav.ung.fordel.handler.FordelProsessTaskTjeneste;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.fordel.handler.WrappedProsessTaskHandler;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;
import no.nav.ung.sak.typer.JournalpostId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;


public abstract class EndeligJournalføringTask extends WrappedProsessTaskHandler {
    private static final Logger log = LoggerFactory.getLogger(EndeligJournalføringTask.class);

    private final TilJournalføringTjeneste journalføringTjeneste;
    private final JournalpostRepository journalpostRepository;


    public EndeligJournalføringTask(FordelProsessTaskTjeneste fordelProsessTaskTjeneste,
                                    TilJournalføringTjeneste journalTjeneste,
                                    JournalpostRepository journalpostRepository) {
        super(fordelProsessTaskTjeneste);
        this.journalføringTjeneste = journalTjeneste;
        this.journalpostRepository = journalpostRepository;
    }

    protected abstract String endeligJouralføringTask();

    protected abstract String sendtInnDokumentTask();

    @Override
    public void precondition(MottattMelding dataWrapper) {
        if (dataWrapper.getSaksnummer().isEmpty()) {
            throw new IllegalStateException("Mangler påkrevd saksnummer.");
        }
        if (dataWrapper.getAktørId().isEmpty()) {
            throw new IllegalStateException("Mangler påkrevd aktørId.");
        }
        if (dataWrapper.getTema() == null) {
            throw new IllegalStateException("Mangler påkrevd tema.");
        }
    }

    @Override
    public MottattMelding doTask(MottattMelding dataWrapper) {
        final var journalpostIder = dataWrapper.getJournalPostIder();
        final var endeligJournalførteJournalPostIder = dataWrapper.getEndeligJournalførteJournalPostIder();
        final var journalpostId = utledNesteJournalpostId(journalpostIder, endeligJournalførteJournalPostIder);
        final var saksnummer = dataWrapper.getSaksnummer();

        if (journalpostId != null && journalføringTjeneste.erAlleredeJournalført(journalpostId)) {
            log.info("Journalpost med id={} er allerede journaført på sak={}", journalpostId, saksnummer.orElse("UNDEFINED"));
            journalpostRepository.markerJournalposterBehandlet(journalpostId);

            var oppdatertPoster = new HashSet<>(endeligJournalførteJournalPostIder);
            oppdatertPoster.add(journalpostId);
            dataWrapper.setEndeligJournalførteJournalPostIder(oppdatertPoster);
        } else {
            try {
                if (journalpostId != null && !journalføringTjeneste.tilJournalføring(journalpostId,
                    saksnummer,
                    dataWrapper.getTema(),
                    dataWrapper.getAktørId().orElseThrow())) {
                    log.info("Får ikke ferdigstilt journalpost med id={} på sak={}", journalpostId, saksnummer.orElse("UNDEFINED"));
                    // TODO: Vurder hva vi gjør med forsendelsen når det er flere her som ikke
                    throw new IllegalStateException("Har mangler som ikke kan fikses opp maskinelt");
                } else if (journalpostId != null) {
                    var oppdatertPoster = new HashSet<>(endeligJournalførteJournalPostIder);
                    oppdatertPoster.add(journalpostId);
                    dataWrapper.setEndeligJournalførteJournalPostIder(oppdatertPoster);
                }
            } catch (DokarkivException e) {
                /*
                 * Stygt, men manglende felt tilbys ikke som strukturerte data ... så eneste måten å hente ut
                 * dette på er strengsøk. Arkiv jobber med å legge til den samme informasjonen strukturert.
                 */
                if (!e.getMessage().contains("Kan ikke ferdigstille journalpost, følgende felt(er) mangler")) {
                    throw e;
                }
                throw new IllegalStateException("Kan ikke ferdigstille journalpost. Vurder om løsningen skal utvides med opprettelse av oppgave i Gosys");
            }
        }

        if (dataWrapper.getEndeligJournalførteJournalPostIder().containsAll(journalpostIder)) {
            log.info("Ferdigstilt journalpost med id={} på sak={}", journalpostId, saksnummer.orElse("UNDEFINED"));
            return dataWrapper.nesteSteg(sendtInnDokumentTask());

        } else {
            log.info("Ferdigstilt journalpost med id={} på sak={}, prøver resterende {} journalposter. ", journalpostId, saksnummer.orElse("UNDEFINED"), journalpostIder.size() - dataWrapper.getEndeligJournalførteJournalPostIder().size());
            return dataWrapper.nesteSteg(endeligJouralføringTask());
        }
    }

    private JournalpostId utledNesteJournalpostId(Set<JournalpostId> journalpostIder, Set<JournalpostId> endeligJournalførteJournalPostIder) {
        for (JournalpostId journalpostId : journalpostIder) {
            if (!endeligJournalførteJournalPostIder.contains(journalpostId)) {
                return journalpostId;
            }
        }
        return null;
    }
}
