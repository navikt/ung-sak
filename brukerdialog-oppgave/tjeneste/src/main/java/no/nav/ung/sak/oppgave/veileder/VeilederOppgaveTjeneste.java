package no.nav.ung.sak.oppgave.veileder;

import no.nav.ung.sak.oppgave.kontrakt.OpprettSøkYtelseOppgaveDto;

/**
 * Interface for veileder-tjeneste som kan opprette oppgaver for brukere.
 * Brukes av veiledere/saksbehandlere for å manuelt opprette oppgaver.
 */
public interface VeilederOppgaveTjeneste {

    /**
     * Oppretter en søk ytelse oppgave for en bruker.
     *
     * @param oppgaveDto DTO med informasjon om oppgaven som skal opprettes
     */
    void opprettSøkYtelseOppgave(OpprettSøkYtelseOppgaveDto oppgaveDto);
}
