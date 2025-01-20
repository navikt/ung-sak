package no.nav.ung.domenetjenester.arkiv.journalpostvurderer;



import static no.nav.ung.domenetjenester.arkiv.journalpostvurderer.VurdertJournalpost.håndtert;
import static no.nav.ung.domenetjenester.arkiv.journalpostvurderer.VurdertJournalpost.ikkeHåndtert;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.domenetjenester.arkiv.JournalføringHendelsetype;
import no.nav.ung.fordel.repo.journalpost.JournalpostRepository;

@ApplicationScoped
public class AlleredeMottattJournalpost implements Journalpostvurderer {

    private static final Logger log = LoggerFactory.getLogger(AlleredeMottattJournalpost.class);

    private static final Set<JournalføringHendelsetype> relevanteHendelsetyper = Set.of(
            JournalføringHendelsetype.MOTTATT,
            JournalføringHendelsetype.TEMA_ENDRET,
            JournalføringHendelsetype.ENDELING_JOURNALFØRT
    );

    private JournalpostRepository journalpostRepository;

    public AlleredeMottattJournalpost() {}

    @Inject
    public AlleredeMottattJournalpost(
            JournalpostRepository journalpostRepository) {
        this.journalpostRepository = journalpostRepository;
    }

    @Override
    public Set<JournalføringHendelsetype> relevanteHendelsetyper() {
        return relevanteHendelsetyper;
    }

    @Override
    public VurdertJournalpost gjørVurdering(Vurderingsgrunnlag vurderingsgrunnlag) {
        var journalPostId = vurderingsgrunnlag.melding().getJournalPostId();

        var mottattJournalpost = journalpostRepository.finnJournalpostMottatt(journalPostId);

        if (mottattJournalpost.isPresent()) {
            // allerede mottatt - fortsetter ikke her
            log.info(vurderingsgrunnlag.logMelding("Allerede mottatt, ignorerer her"));
            return håndtert();
        }

        return ikkeHåndtert();
    }
}
