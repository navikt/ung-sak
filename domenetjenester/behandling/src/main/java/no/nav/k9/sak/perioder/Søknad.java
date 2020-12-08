package no.nav.k9.sak.perioder;

import no.nav.k9.sak.typer.JournalpostId;

import java.time.LocalDateTime;
import java.util.Objects;

public class Søknad {

    private JournalpostId journalpostId;
    private LocalDateTime innsendingsTidspunkt;

    public Søknad(JournalpostId journalpostId, LocalDateTime innsendingsTidspunkt) {
        this.journalpostId = journalpostId;
        this.innsendingsTidspunkt = innsendingsTidspunkt;
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
            Objects.equals(innsendingsTidspunkt, that.innsendingsTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, innsendingsTidspunkt);
    }

    @Override
    public String toString() {
        return "SøktnadsDokument{" +
            "journalpostId=" + journalpostId +
            ", innsendingsTidspunkt=" + innsendingsTidspunkt +
            '}';
    }
}
