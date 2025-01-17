package no.nav.ung.domenetjenester.arkiv.journalpostvurderer;


import no.nav.ung.fordel.handler.MottattMelding;

public record VurdertJournalpost(MottattMelding håndtertMelding, Boolean erHåndtert) {
    public static VurdertJournalpost ikkeHåndtert() {
        return new VurdertJournalpost(null,false);
    }
    public static VurdertJournalpost håndtert() {
        return new VurdertJournalpost(null, true);
    }
    public static VurdertJournalpost håndtert(MottattMelding håndtertMelding) {
        return new VurdertJournalpost(håndtertMelding, true);
    }
}
