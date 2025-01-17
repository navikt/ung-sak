package no.nav.ung.domenetjenester.arkiv.journalpostvurderer;


import no.nav.ung.domenetjenester.arkiv.JournalføringHendelsetype;
import no.nav.ung.domenetjenester.arkiv.JournalpostInfo;
import no.nav.ung.fordel.handler.MottattMelding;

record Vurderingsgrunnlag(
        MottattMelding melding,
        JournalpostInfo journalpostInfo) {
    // TODO: Endre til UNG når dette støttes
    private static final String FAGSAK_SYSTEM_UNG = "K9";

    boolean erEndeligJournalført() {
        return JournalføringHendelsetype.ENDELING_JOURNALFØRT == melding.getJournalføringHendelsetype().orElse(null);
    }

    boolean erMottatt() {
        return JournalføringHendelsetype.MOTTATT == melding.getJournalføringHendelsetype().orElse(null);
    }

    boolean tilhørerUng() {
        return FAGSAK_SYSTEM_UNG.equals(journalpostInfo.getFagsakSystem().orElse(null)) &&
                journalpostInfo.getFagsakId().isPresent();
    }

    String logMelding(String msg) {
        if (erEndeligJournalført()) {
            return String.format("%s. Journalpost[%s], Tema=%s, Brevkode=%s, erStrukturert=%s, FagsakSystem=%s, FagsakId=%s",
                    msg, melding.getJournalPostId().getVerdi(), melding.getTema().getOffisiellKode(), journalpostInfo.getBrevkode(), journalpostInfo.getInnholderStrukturertInformasjon(),
                    journalpostInfo.getFagsakSystem().orElse(null), journalpostInfo.getFagsakId().orElse(null));

        } else {
            return String.format("%s. Journalpost[%s], Tema=%s, Brevkode=%s, erStrukturert=%s",
                    msg, melding.getJournalPostId().getVerdi(), melding.getTema().getOffisiellKode(), journalpostInfo.getBrevkode(), journalpostInfo.getInnholderStrukturertInformasjon()
            );
        }
    }
}
