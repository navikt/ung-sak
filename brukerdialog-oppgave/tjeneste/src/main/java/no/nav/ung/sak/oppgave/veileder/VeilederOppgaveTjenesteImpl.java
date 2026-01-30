package no.nav.ung.sak.oppgave.veileder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveLivssyklusTjeneste;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettSøkYtelseOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.søkytelse.SøkYtelseOppgavetypeDataDTO;
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
    public VeilederOppgaveTjenesteImpl() {
        // CDI proxy
    }

    @Inject
    public VeilederOppgaveTjenesteImpl(OppgaveLivssyklusTjeneste oppgaveLivssyklusTjeneste) {
        this.oppgaveLivssyklusTjeneste = oppgaveLivssyklusTjeneste;
    }

    @Override
    public void opprettSøkYtelseOppgave(OpprettSøkYtelseOppgaveDto oppgaveDto) {
        AktørId aktørId = new AktørId(oppgaveDto.aktørId());
        SøkYtelseOppgavetypeDataDTO oppgaveData = new SøkYtelseOppgavetypeDataDTO(oppgaveDto.fomDato());

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

        oppgaveLivssyklusTjeneste.opprettOppgave(oppgave);
    }
}

