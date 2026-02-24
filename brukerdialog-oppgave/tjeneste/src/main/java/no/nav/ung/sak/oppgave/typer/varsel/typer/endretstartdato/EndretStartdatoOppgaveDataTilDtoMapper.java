package no.nav.ung.sak.oppgave.typer.varsel.typer.endretstartdato;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretstartdato.EndretStartdatoDataDto;
import no.nav.ung.sak.oppgave.OppgaveDataEntitetTilDtoMapper;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_ENDRET_STARTDATO)
public class EndretStartdatoOppgaveDataTilDtoMapper implements OppgaveDataEntitetTilDtoMapper {

    protected EndretStartdatoOppgaveDataTilDtoMapper() {
        // CDI proxy
    }

    @Override
    public OppgavetypeDataDto tilDto(OppgaveDataEntitet entitet) {
        var e = (EndretStartdatoOppgaveDataEntitet) entitet;
        return new EndretStartdatoDataDto(e.getNyStartdato(), e.getForrigeStartdato());
    }
}

