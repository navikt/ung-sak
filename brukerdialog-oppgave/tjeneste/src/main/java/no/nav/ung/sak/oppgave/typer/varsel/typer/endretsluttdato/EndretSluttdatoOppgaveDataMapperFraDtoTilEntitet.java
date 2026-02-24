package no.nav.ung.sak.oppgave.typer.varsel.typer.endretsluttdato;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato.EndretSluttdatoDataDto;
import no.nav.ung.sak.oppgave.OppgaveDataMapperFraDtoTilEntitet;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_ENDRET_SLUTTDATO)
public class EndretSluttdatoOppgaveDataMapperFraDtoTilEntitet implements OppgaveDataMapperFraDtoTilEntitet {

    protected EndretSluttdatoOppgaveDataMapperFraDtoTilEntitet() {
        // CDI proxy
    }

    @Override
    public OppgaveDataEntitet map(OppgavetypeDataDto data) {
        var dto = (EndretSluttdatoDataDto) data;
        return new EndretSluttdatoOppgaveDataEntitet(dto.nySluttdato(), dto.forrigeSluttdato());
    }
}
