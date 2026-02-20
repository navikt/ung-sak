package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.sak.kontrakt.oppgaver.EndreOppgaveStatusDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.OpprettEndretPeriodeOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato.OpprettEndretSluttdatoOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretstartdato.OpprettEndretStartdatoOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.OpprettInntektsrapporteringOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.OpprettKontrollerRegisterInntektOppgaveDto;
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

    public void opprettKontrollerRegisterInntektOppgave(OpprettKontrollerRegisterInntektOppgaveDto oppgave) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.opprettKontrollerRegisterInntektOppgave(oppgave));
    }

    public void opprettInntektrapporteringOppgave(OpprettInntektsrapporteringOppgaveDto oppgave) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.opprettInntektrapporteringOppgave(oppgave));
    }

    public void opprettEndretStartdatoOppgave(OpprettEndretStartdatoOppgaveDto oppgave) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.opprettEndretStartdatoOppgave(oppgave));
    }

    public void opprettEndretSluttdatoOppgave(OpprettEndretSluttdatoOppgaveDto oppgave) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.opprettEndretSluttdatoOppgave(oppgave));
    }

    public void opprettEndretPeriodeOppgave(OpprettEndretPeriodeOppgaveDto oppgave) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.opprettEndretPeriodeOppgave(oppgave));
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
