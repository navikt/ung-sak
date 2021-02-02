package no.nav.k9.sak.perioder;

import no.nav.k9.sak.typer.JournalpostId;

import java.time.LocalDateTime;
import java.util.Objects;

public class KravDokument implements Comparable<KravDokument> {

    private JournalpostId journalpostId;
    private KravDokumentType type;
    private LocalDateTime innsendingsTidspunkt;

    public KravDokument(JournalpostId journalpostId, LocalDateTime innsendingsTidspunkt, KravDokumentType type) {
        this.journalpostId = journalpostId;
        this.innsendingsTidspunkt = innsendingsTidspunkt;
        this.type = type;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public KravDokumentType getType() {
        return type;
    }

    public LocalDateTime getInnsendingsTidspunkt() {
        return innsendingsTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KravDokument that = (KravDokument) o;
        return Objects.equals(journalpostId, that.journalpostId) &&
            Objects.equals(type, that.type) &&
            Objects.equals(innsendingsTidspunkt, that.innsendingsTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, type, innsendingsTidspunkt);
    }

    @Override
    public String toString() {
        return "SøktnadsDokument{" +
            "journalpostId=" + journalpostId +
            ", type=" + type +
            ", innsendingsTidspunkt=" + innsendingsTidspunkt +
            '}';
    }

    @Override
    public int compareTo(KravDokument o) {
        return this.innsendingsTidspunkt.compareTo(o.getInnsendingsTidspunkt());
    }
}
