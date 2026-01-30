package no.nav.ung.sak.oppgave.brukerdialog;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveMapper;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveRepository;
import no.nav.ung.sak.oppgave.OppgaveLivssyklusTjeneste;
import no.nav.ung.sak.oppgave.kontrakt.BrukerdialogOppgaveDto;
import no.nav.ung.sak.oppgave.saksbehandling.OppgaveForSaksbehandlingGrensesnittImpl;

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
public class BrukerdialogOppgaveTjenesteImpl implements BrukerdialogOppgaveTjeneste {

    private OppgaveLivssyklusTjeneste oppgaveLivssyklusTjeneste;
    private BrukerdialogOppgaveRepository repository;
    private BrukerdialogOppgaveMapper mapper;

    public BrukerdialogOppgaveTjenesteImpl() {
        // CDI proxy
    }

    @Inject
    public BrukerdialogOppgaveTjenesteImpl(OppgaveLivssyklusTjeneste oppgaveLivssyklusTjeneste, BrukerdialogOppgaveRepository repository, BrukerdialogOppgaveMapper mapper) {
        this.oppgaveLivssyklusTjeneste = oppgaveLivssyklusTjeneste;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<BrukerdialogOppgaveDto> hentAlleOppgaverForAktør(AktørId aktørId) {
        return repository.hentAlleOppgaverForAktør(aktørId).stream()
            .map(mapper::tilDto)
            .collect(Collectors.toList());
    }


    @Override
    public BrukerdialogOppgaveDto hentOppgaveForOppgavereferanse(UUID oppgavereferanse, AktørId aktørId) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(oppgavereferanse, aktørId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + oppgavereferanse));
        return mapper.tilDto(oppgave);
    }

    @Override
    public BrukerdialogOppgaveDto lukkOppgave(UUID oppgavereferanse, AktørId aktørId) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(oppgavereferanse, aktørId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + oppgavereferanse));
        var oppdatertOppgave = repository.lukkOppgave(oppgave);
        return mapper.tilDto(oppdatertOppgave);
    }

    @Override
    public BrukerdialogOppgaveDto åpneOppgave(UUID oppgavereferanse, AktørId aktørId) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(oppgavereferanse, aktørId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + oppgavereferanse));
        var oppdatertOppgave = repository.åpneOppgave(oppgave);
        return mapper.tilDto(oppdatertOppgave);
    }

    @Override
    public BrukerdialogOppgaveDto løsOppgave(UUID oppgavereferanse, AktørId aktørId) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(oppgavereferanse, aktørId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + oppgavereferanse));
        var oppdatertOppgave = oppgaveLivssyklusTjeneste.løsOppgave(oppgave);
        return mapper.tilDto(oppdatertOppgave);
    }
}

