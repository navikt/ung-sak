package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.EndretPeriodeOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndreStatusDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
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

    public void opprettKontrollerRegisterInntektOppgave(RegisterInntektOppgaveDTO oppgave) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.opprettKontrollerRegisterInntektOppgave(oppgave));
    }

    public void opprettInntektrapporteringOppgave(InntektsrapporteringOppgaveDTO oppgave) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.opprettInntektrapporteringOppgave(oppgave));
    }

    public void opprettEndretStartdatoOppgave(EndretStartdatoOppgaveDTO oppgave) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.opprettEndretStartdatoOppgave(oppgave));
    }

    public void opprettEndretSluttdatoOppgave(EndretSluttdatoOppgaveDTO oppgave) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.opprettEndretSluttdatoOppgave(oppgave));
    }

    public void opprettEndretPeriodeOppgave(EndretPeriodeOppgaveDTO oppgave) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.opprettEndretPeriodeOppgave(oppgave));
    }

    public void avbrytOppgave(UUID eksternRef) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.avbrytOppgave(eksternRef));
    }

    public void oppgaveUtløpt(UUID eksternRef) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.oppgaveUtløpt(eksternRef));
    }

    public void settOppgaveTilUtløpt(EndreStatusDTO dto) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.settOppgaveTilUtløpt(dto));
    }

    public void settOppgaveTilAvbrutt(EndreStatusDTO dto) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.settOppgaveTilAvbrutt(dto));
    }

    public void løsSøkYtelseOppgave(DeltakerDTO deltakerDTO) {
        instanser.stream().filter(OppgaveForSaksbehandlingGrensesnitt::isEnabled).forEach(it -> it.løsSøkYtelseOppgave(deltakerDTO));
    }
}
