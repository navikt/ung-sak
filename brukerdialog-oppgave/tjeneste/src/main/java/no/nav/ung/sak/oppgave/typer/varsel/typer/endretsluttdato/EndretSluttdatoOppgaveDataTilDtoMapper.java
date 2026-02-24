package no.nav.ung.sak.oppgave.typer.varsel.typer.endretsluttdato;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato.EndretSluttdatoDataDto;
import no.nav.ung.sak.oppgave.OppgaveDataEntitetTilDtoMapper;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_ENDRET_SLUTTDATO)
public class EndretSluttdatoOppgaveDataTilDtoMapper implements OppgaveDataEntitetTilDtoMapper {

    protected EndretSluttdatoOppgaveDataTilDtoMapper() {
        // CDI proxy
    }

    @Override
    public OppgavetypeDataDto tilDto(OppgaveDataEntitet entitet) {
        var e = (EndretSluttdatoOppgaveDataEntitet) entitet;
        return new EndretSluttdatoDataDto(e.getNySluttdato(), e.getForrigeSluttdato());
    }
}

