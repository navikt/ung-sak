package no.nav.ung.sak.oppgave.veileder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.kontrakt.oppgaver.BrukerdialogOppgaveDto;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveMapper;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveLivssyklusTjeneste;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettSøkYtelseOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.søkytelse.SøkYtelseOppgavetypeDataDto;
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

    private OppgaveLivssyklusTjeneste oppgaveLivssyklusTjeneste;
    private BrukerdialogOppgaveMapper brukerdialogOppgaveMapper;

    public VeilederOppgaveTjenesteImpl() {
        // CDI proxy
    }

    @Inject
    public VeilederOppgaveTjenesteImpl(OppgaveLivssyklusTjeneste oppgaveLivssyklusTjeneste, BrukerdialogOppgaveMapper brukerdialogOppgaveMapper) {
        this.oppgaveLivssyklusTjeneste = oppgaveLivssyklusTjeneste;
        this.brukerdialogOppgaveMapper = brukerdialogOppgaveMapper;
    }

    @Override
    public BrukerdialogOppgaveDto opprettSøkYtelseOppgave(OpprettSøkYtelseOppgaveDto oppgaveDto) {
        SøkYtelseOppgavetypeDataDto oppgaveData = new SøkYtelseOppgavetypeDataDto(oppgaveDto.fomDato());

        // Generer UUID hvis ikke oppgitt
        UUID oppgaveReferanse = oppgaveDto.oppgaveReferanse() != null
            ? oppgaveDto.oppgaveReferanse()
            : UUID.randomUUID();

        BrukerdialogOppgaveEntitet oppgave = new BrukerdialogOppgaveEntitet(
            oppgaveReferanse,
            OppgaveType.SØK_YTELSE,
            oppgaveDto.aktørId(),
            oppgaveData,
            null // Ingen frist for søk ytelse oppgave
        );

        oppgaveLivssyklusTjeneste.opprettOppgave(oppgave, oppgave.oppgavetypeData());

        return brukerdialogOppgaveMapper.tilDto(oppgave);

    }
}

