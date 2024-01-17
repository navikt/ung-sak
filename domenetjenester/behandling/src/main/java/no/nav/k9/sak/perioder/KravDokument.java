package no.nav.k9.sak.perioder;

import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.søknad.felles.Kildesystem;

import java.time.LocalDateTime;
import java.util.Objects;

public class KravDokument implements Comparable<KravDokument> {

    private final JournalpostId journalpostId;
    private final KravDokumentType type;
    private final LocalDateTime innsendingsTidspunkt;
    private final Kildesystem kildesystem;

    public KravDokument(JournalpostId journalpostId, LocalDateTime innsendingsTidspunkt, KravDokumentType type) {
        this(journalpostId, innsendingsTidspunkt, type, null);
    }

    public KravDokument(JournalpostId journalpostId, LocalDateTime innsendingsTidspunkt, KravDokumentType type, String kildesystem) {
        this.journalpostId = journalpostId;
        this.innsendingsTidspunkt = innsendingsTidspunkt;
        this.type = type;
        this.kildesystem = Kildesystem.of(kildesystem);
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

    public Kildesystem getKildesystem() {
        return kildesystem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KravDokument that = (KravDokument) o;
        return Objects.equals(journalpostId, that.journalpostId) &&
            Objects.equals(type, that.type) &&
            Objects.equals(kildesystem.getKode(), that.kildesystem.getKode()) &&
            Objects.equals(innsendingsTidspunkt, that.innsendingsTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, type, innsendingsTidspunkt, kildesystem);
    }

    @Override
    public String toString() {
        return "KravDokument{" +
            "journalpostId=" + journalpostId +
            ", type=" + type +
            ", innsendingsTidspunkt=" + innsendingsTidspunkt +
            ", kildesystem=" + kildesystem +
            '}';
    }

    @Override
    public int compareTo(KravDokument o) {
        return this.innsendingsTidspunkt.compareTo(o.getInnsendingsTidspunkt());
    }
}
