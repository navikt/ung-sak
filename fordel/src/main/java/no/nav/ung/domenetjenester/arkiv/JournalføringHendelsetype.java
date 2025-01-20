package no.nav.ung.domenetjenester.arkiv;

import java.util.Optional;

public enum JournalføringHendelsetype {
    MOTTATT("JournalpostMottatt"),
    TEMA_ENDRET("TemaEndret"),
    ENDELING_JOURNALFØRT("EndeligJournalført");

    public final String kode;

    JournalføringHendelsetype(String kode) {
        this.kode = kode;
    }

    public static Optional<JournalføringHendelsetype> fraKode(String kode) {
        for (JournalføringHendelsetype type : values()) {
            if (type.kode.equals(kode)) return Optional.of(type);
        }
        return Optional.empty();
    }
}
