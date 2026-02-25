package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.sak.kontrakt.oppgaver.EndreOppgaveStatusDto;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.sak.oppgave.OppgaveForSaksbehandlingGrensesnitt;

import java.util.UUID;

/**
 * Midlertidig tjeneste for delegering av oppgaver til enten intern eller ekstern oppgavehåndtering.
 */
@ApplicationScoped
public class MidlertidigOppgaveDelegeringTjeneste {

    private Instance<OppgaveForSaksbehandlingGrensesnitt> instanser;

    public MidlertidigOppgaveDelegeringTjeneste() {
    }

    @Inject
    public MidlertidigOppgaveDelegeringTjeneste(@Any Instance<OppgaveForSaksbehandlingGrensesnitt> instanser) {
        this.instanser = instanser;
    }

    public void opprettOppgave(OpprettOppgaveDto oppgave) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.opprettOppgave(oppgave));
    }

    public void avbrytOppgave(UUID eksternRef) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.avbrytOppgave(eksternRef));
    }

    public void oppgaveUtløpt(UUID eksternRef) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.oppgaveUtløpt(eksternRef));
    }

    public void settOppgaveTilUtløpt(EndreOppgaveStatusDto dto) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.settOppgaveTilUtløpt(dto));
    }

    public void settOppgaveTilAvbrutt(EndreOppgaveStatusDto dto) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.settOppgaveTilAvbrutt(dto));
    }

    public void løsSøkYtelseOppgave(String deltakerIdent) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.løsSøkYtelseOppgave(deltakerIdent));
    }
}
