package no.nav.ung.sak.etterlysning;

import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveRequest;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.EndreOppgaveStatusDto;
import no.nav.ung.brukerdialog.typer.AktørId;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface for å opprette og administrere brukerdialog-oppgaver.
 * Implementeres av både UngOppgaveKlient (REST-klient til ekstern tjeneste)
 * og BrukerdialogOppgaveTjeneste (intern håndtering i samme applikasjon).
 */
public interface OppgaveForSaksbehandlingGrensesnitt {

    default boolean isEnabled() {
        return true;
    }

    /**
     * Oppretter en oppgave. Oppgavetypen bestemmes av {@code oppgave.oppgavetypeData()}.
     */
    void opprettOppgave(OpprettOppgaveDto oppgave);

    /**
     * Avbryter en oppgave basert på ekstern referanse.
     */
    void avbrytOppgave(OppgaveRequest eksternRef);

    /**
     * Markerer en oppgave som utløpt basert på ekstern referanse.
     */
    void oppgaveUtløpt(OppgaveRequest eksternRef);

    /**
     * Setter oppgaver av en gitt type og periode til utløpt.
     */
    void settOppgaveTilUtløpt(EndreOppgaveStatusDto dto);

    /**
     * Setter oppgaver av en gitt type og periode til avbrutt.
     */
    void settOppgaveTilAvbrutt(EndreOppgaveStatusDto dto);

    /**
     * Løser en søk-ytelse-oppgave.
     *
     * @param aktørId aktørId for bruker
     */
    void løsSøkYtelseOppgave(AktørId aktørId);

    /**
     * Endrer frist for en oppgave.
     *
     * @param aktørId      aktørId for den oppgaven gjelder
     * @param eksternReferanse oppgavereferanse
     * @param frist            ny frist for oppgaven
     */
    void endreFrist(AktørId aktørId, UUID eksternReferanse, LocalDateTime frist);
}
