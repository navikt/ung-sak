package no.nav.ung.domenetjenester.arkiv.journalpostvurderer;


import static no.nav.ung.domenetjenester.arkiv.journalpostvurderer.VurdertJournalpost.ikkeHåndtert;

import java.util.Set;

import no.nav.ung.domenetjenester.arkiv.JournalføringHendelsetype;
import no.nav.ung.domenetjenester.arkiv.JournalpostInfo;
import no.nav.ung.fordel.handler.MottattMelding;


public interface Journalpostvurderer {
    Set<JournalføringHendelsetype> mottattOgTemaEndret = Set.of(
            JournalføringHendelsetype.MOTTATT,
            JournalføringHendelsetype.TEMA_ENDRET
    );

    default VurdertJournalpost vurder(MottattMelding melding, JournalpostInfo journalpostInfo) {
        var vurderingsgrunnlag = new Vurderingsgrunnlag(melding, journalpostInfo);
        if (erRelevantHendelsetype(melding) && skalVurdere(vurderingsgrunnlag)) {
            return gjørVurdering(vurderingsgrunnlag);
        } else {
            return ikkeHåndtert();
        }
    }

    private boolean erRelevantHendelsetype(MottattMelding melding) {
        var hendelsetype = melding.getJournalføringHendelsetype();
        var relevanteHendelsetyper = relevanteHendelsetyper();

        if (hendelsetype.isEmpty()) {
            // Kun i en overgangsfase når vi begynner å lytte på 'EndeligJournalført' at dette er aktuelt.
            // Når den ikke er satt er det enten 'MidlertidigJournalført' eller 'TemaEndret'
            return relevanteHendelsetyper.contains(JournalføringHendelsetype.MOTTATT) ||
                    relevanteHendelsetyper.contains(JournalføringHendelsetype.TEMA_ENDRET);
        } else {
            return relevanteHendelsetyper.contains(hendelsetype.get());
        }
    }

    VurdertJournalpost gjørVurdering(Vurderingsgrunnlag vurderingsgrunnlag);
    default boolean skalVurdere(Vurderingsgrunnlag vurderingsgrunnlag) {
        return true;
    }
    default Set<JournalføringHendelsetype> relevanteHendelsetyper() {
        return mottattOgTemaEndret;
    }
}
