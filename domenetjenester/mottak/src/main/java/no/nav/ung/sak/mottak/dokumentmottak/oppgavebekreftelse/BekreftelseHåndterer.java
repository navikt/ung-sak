package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import no.nav.ung.sak.mottak.dokumentmottak.Trigger;

import java.util.Optional;
import java.util.UUID;

public interface BekreftelseHåndterer {

    void håndter(OppgaveBekreftelseInnhold bekreftelse);

    Optional<Trigger> utledTrigger(UUID oppgaveId);
}
