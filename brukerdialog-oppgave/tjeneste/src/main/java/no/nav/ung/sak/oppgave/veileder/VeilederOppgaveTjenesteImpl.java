package no.nav.ung.sak.oppgave.veileder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.kontrakt.oppgaver.BrukerdialogOppgaveDto;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveMapper;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveLivssyklusTjeneste;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettOppgaveDto;
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
    public BrukerdialogOppgaveDto opprettSøkYtelseOppgave(OpprettOppgaveDto oppgaveDto) {
        UUID oppgaveReferanse = oppgaveDto.oppgaveReferanse() != null
            ? oppgaveDto.oppgaveReferanse()
            : UUID.randomUUID();

        BrukerdialogOppgaveEntitet oppgave = new BrukerdialogOppgaveEntitet(
            oppgaveReferanse,
            OppgaveType.SØK_YTELSE,
            oppgaveDto.aktørId(),
            oppgaveDto.frist()
        );

        oppgaveLivssyklusTjeneste.opprettOppgave(oppgave, oppgaveDto.oppgavetypeData());

        return brukerdialogOppgaveMapper.tilDto(oppgave);

    }
}

