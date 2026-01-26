package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.EndretPeriodeOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndreStatusDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;

import java.util.UUID;

/**
 * Tjeneste for å opprette og administrere brukerdialog-oppgaver.
 * Implementerer BrukerdialogOppgaveService interfacet.
 *
 * Denne tjenesten brukes av etterlysning-modulen og andre moduler
 * som trenger å opprette oppgaver internt i applikasjonen.
 */
@ApplicationScoped
public class OppgaveForSaksbehandlingGrensesnittImpl implements OppgaveForSaksbehandlingGrensesnitt {

    private BrukerdialogOppgaveRepository repository;

    public OppgaveForSaksbehandlingGrensesnittImpl() {
        // CDI proxy
    }

    @Inject
    public OppgaveForSaksbehandlingGrensesnittImpl(BrukerdialogOppgaveRepository repository) {
        this.repository = repository;
    }

    @Override
    public void opprettKontrollerRegisterInntektOppgave(RegisterInntektOppgaveDTO oppgave) {
        // TODO: Implementer opprettelse av kontroller registerinntekt oppgave
        throw new UnsupportedOperationException("Ikke implementert ennå");
    }

    @Override
    public void opprettInntektrapporteringOppgave(InntektsrapporteringOppgaveDTO oppgave) {
        // TODO: Implementer opprettelse av inntektsrapportering oppgave
        throw new UnsupportedOperationException("Ikke implementert ennå");
    }

    @Override
    public void opprettEndretStartdatoOppgave(EndretStartdatoOppgaveDTO oppgave) {
        // TODO: Implementer opprettelse av endret startdato oppgave
        throw new UnsupportedOperationException("Ikke implementert ennå");
    }

    @Override
    public void opprettEndretSluttdatoOppgave(EndretSluttdatoOppgaveDTO oppgave) {
        // TODO: Implementer opprettelse av endret sluttdato oppgave
        throw new UnsupportedOperationException("Ikke implementert ennå");
    }

    @Override
    public void opprettEndretPeriodeOppgave(EndretPeriodeOppgaveDTO oppgave) {
        // TODO: Implementer opprettelse av endret periode oppgave
        throw new UnsupportedOperationException("Ikke implementert ennå");
    }

    @Override
    public void avbrytOppgave(UUID eksternRef) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(eksternRef)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + eksternRef));

        oppgave.setStatus(OppgaveStatus.AVBRUTT);
        repository.oppdater(oppgave);
    }

    @Override
    public void oppgaveUtløpt(UUID eksternRef) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(eksternRef)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + eksternRef));

        oppgave.setStatus(OppgaveStatus.UTLØPT);
        repository.oppdater(oppgave);
    }

    @Override
    public void settOppgaveTilUtløpt(EndreStatusDTO dto) {
        // TODO: Implementer setting av oppgaver til utløpt basert på type og periode
        throw new UnsupportedOperationException("Ikke implementert ennå");
    }

    @Override
    public void settOppgaveTilAvbrutt(EndreStatusDTO dto) {
        // TODO: Implementer setting av oppgaver til avbrutt basert på type og periode
        throw new UnsupportedOperationException("Ikke implementert ennå");
    }

    @Override
    public void løsSøkYtelseOppgave(DeltakerDTO deltakerDTO) {
        // TODO: Implementer løsning av søk-ytelse-oppgave
        throw new UnsupportedOperationException("Ikke implementert ennå");
    }
}

