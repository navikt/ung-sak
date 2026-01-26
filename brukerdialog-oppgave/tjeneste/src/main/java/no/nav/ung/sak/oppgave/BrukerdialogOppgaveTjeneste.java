package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.kontrakt.oppgave.BrukerdialogOppgaveDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tjeneste for å hente og vise brukerdialog-oppgaver.
 * Brukes primært av REST-tjenester for å hente oppgaver til visning.
 *
 * For å opprette og administrere oppgaver, bruk {@link OppgaveForSaksbehandlingGrensesnittImpl}.
 */
@ApplicationScoped
public class BrukerdialogOppgaveTjeneste {

    private BrukerdialogOppgaveRepository repository;
    private BrukerdialogOppgaveMapper mapper;

    public BrukerdialogOppgaveTjeneste() {
        // CDI proxy
    }

    @Inject
    public BrukerdialogOppgaveTjeneste(BrukerdialogOppgaveRepository repository, BrukerdialogOppgaveMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<BrukerdialogOppgaveDto> hentAlleOppgaverForAktør(AktørId aktørId) {
        return repository.hentAlleOppgaverForAktør(aktørId).stream()
            .map(mapper::tilDto)
            .collect(Collectors.toList());
    }

    public List<BrukerdialogOppgaveDto> hentAlleVarslerForAktør(AktørId aktørId) {
        return repository.hentAlleVarslerForAktør(aktørId).stream()
            .map(mapper::tilDto)
            .collect(Collectors.toList());
    }

    public List<BrukerdialogOppgaveDto> hentAlleSøknaderForAktør(AktørId aktørId) {
        return repository.hentAlleSøknaderForAktør(aktørId).stream()
            .map(mapper::tilDto)
            .collect(Collectors.toList());
    }

    public BrukerdialogOppgaveDto hentOppgaveForOppgavereferanse(UUID oppgavereferanse) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(oppgavereferanse)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + oppgavereferanse));
        return mapper.tilDto(oppgave);
    }

    public BrukerdialogOppgaveDto lukkOppgave(UUID oppgavereferanse) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(oppgavereferanse)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + oppgavereferanse));

        oppgave.setStatus(OppgaveStatus.LUKKET);
        oppgave.setLukketDato(LocalDateTime.now());

        var oppdatertOppgave = repository.oppdater(oppgave);
        return mapper.tilDto(oppdatertOppgave);
    }

    public BrukerdialogOppgaveDto åpneOppgave(UUID oppgavereferanse) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(oppgavereferanse)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + oppgavereferanse));

        oppgave.setÅpnetDato(LocalDateTime.now());

        var oppdatertOppgave = repository.oppdater(oppgave);
        return mapper.tilDto(oppdatertOppgave);
    }

    public BrukerdialogOppgaveDto løsOppgave(UUID oppgavereferanse) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(oppgavereferanse)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + oppgavereferanse));

        oppgave.setStatus(OppgaveStatus.LØST);
        oppgave.setLøstDato(LocalDateTime.now());

        var oppdatertOppgave = repository.oppdater(oppgave);
        return mapper.tilDto(oppdatertOppgave);
    }
}

