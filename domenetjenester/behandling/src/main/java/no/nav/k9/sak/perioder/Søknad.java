package no.nav.k9.sak.perioder;

import no.nav.k9.sak.typer.JournalpostId;

import java.time.LocalDateTime;
import java.util.Objects;

public class Søknad implements Comparable<Søknad> {

    private JournalpostId journalpostId;
    private SøknadType type;
    private LocalDateTime innsendingsTidspunkt;

    public Søknad(JournalpostId journalpostId, LocalDateTime innsendingsTidspunkt, SøknadType type) {
        this.journalpostId = journalpostId;
        this.innsendingsTidspunkt = innsendingsTidspunkt;
        this.type = type;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public LocalDateTime getInnsendingsTidspunkt() {
        return innsendingsTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Søknad that = (Søknad) o;
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
    public int compareTo(Søknad o) {
        return this.innsendingsTidspunkt.compareTo(o.getInnsendingsTidspunkt());
    }
}
