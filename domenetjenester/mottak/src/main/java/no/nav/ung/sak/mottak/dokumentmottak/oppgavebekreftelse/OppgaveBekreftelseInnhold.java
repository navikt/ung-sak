package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import no.nav.k9.oppgave.OppgaveBekreftelse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.typer.JournalpostId;

public record OppgaveBekreftelseInnhold(
    JournalpostId journalpostId, Behandling behandling, OppgaveBekreftelse oppgaveBekreftelse
) {
}
