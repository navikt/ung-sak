package no.nav.ung.sak.oppgave.veileder;

import no.nav.ung.sak.kontrakt.oppgaver.BrukerdialogOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettOppgaveDto;

/**
 * Interface for veileder-tjeneste som kan opprette oppgaver for brukere.
 * Brukes av veiledere/saksbehandlere for å manuelt opprette oppgaver.
 */
public interface VeilederOppgaveTjeneste {

    /**
     * Oppretter en søk ytelse oppgave for en bruker.
     *
     * @param oppgaveDto DTO med informasjon om oppgaven som skal opprettes
     * @return
     */
    BrukerdialogOppgaveDto opprettSøkYtelseOppgave(OpprettOppgaveDto oppgaveDto);
}
