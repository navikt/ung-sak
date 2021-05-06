package no.nav.k9.sak.domene.opptjening;

import java.time.LocalDateTime;
import java.util.Objects;

import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.typer.JournalpostId;

class WrappedOppgittOpptjening {
    private JournalpostId journalpostId;
    private LocalDateTime innsendingstidspunkt;

    private OppgittOpptjening raw;

    WrappedOppgittOpptjening(JournalpostId journalpostId, LocalDateTime innsendingstidspunkt, OppgittOpptjening raw) {
        this.journalpostId = Objects.requireNonNull(journalpostId);
        this.innsendingstidspunkt = Objects.requireNonNull(innsendingstidspunkt);
        this.raw = raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedOppgittOpptjening that = (WrappedOppgittOpptjening) o;
        return Objects.equals(journalpostId, that.journalpostId)
            && Objects.equals(innsendingstidspunkt, that.innsendingstidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, innsendingstidspunkt);
    }

    JournalpostId getJournalpostId() {
        return journalpostId;
    }

    LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    OppgittOpptjening getRaw() {
        return raw;
    }
}
