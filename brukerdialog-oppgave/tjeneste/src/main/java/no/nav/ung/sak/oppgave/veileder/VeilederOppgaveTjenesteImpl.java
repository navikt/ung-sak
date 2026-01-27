package no.nav.ung.sak.oppgave.veileder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveRepository;
import no.nav.ung.sak.oppgave.OppgaveType;
import no.nav.ung.sak.oppgave.kontrakt.OpprettSøkYtelseOppgaveDto;
import no.nav.ung.sak.oppgave.typer.søkytelse.SøkYtelseOppgaveData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Implementasjon av VeilederOppgaveTjeneste.
 * Håndterer opprettelse av oppgaver fra veiledere/saksbehandlere.
 */
@ApplicationScoped
public class VeilederOppgaveTjenesteImpl implements VeilederOppgaveTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(VeilederOppgaveTjenesteImpl.class);

    private BrukerdialogOppgaveRepository repository;

    public VeilederOppgaveTjenesteImpl() {
        // CDI proxy
    }

    @Inject
    public VeilederOppgaveTjenesteImpl(BrukerdialogOppgaveRepository repository) {
        this.repository = repository;
    }

    @Override
    public void opprettSøkYtelseOppgave(OpprettSøkYtelseOppgaveDto oppgaveDto) {
        AktørId aktørId = new AktørId(oppgaveDto.aktørId());
        SøkYtelseOppgaveData oppgaveData = new SøkYtelseOppgaveData(oppgaveDto.fomDato());

        // Generer UUID hvis ikke oppgitt
        UUID oppgaveReferanse = oppgaveDto.oppgaveReferanse() != null
            ? oppgaveDto.oppgaveReferanse()
            : UUID.randomUUID();

        BrukerdialogOppgaveEntitet oppgave = new BrukerdialogOppgaveEntitet(
            oppgaveReferanse,
            OppgaveType.SØK_YTELSE,
            aktørId,
            oppgaveData,
            null // Ingen frist for søk ytelse oppgave
        );

        repository.persister(oppgave);
    }
}

