package no.nav.ung.sak.oppgave.typer.oppgave.søkytelse;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.søkytelse.SøkYtelseOppgavetypeDataDto;
import no.nav.ung.sak.oppgave.OppgaveDataMapperFraEntitetTilDto;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.SØK_YTELSE)
public class SøkYtelseOppgaveDataTilDtoMapperFraEntitetTilDto implements OppgaveDataMapperFraEntitetTilDto {

    protected SøkYtelseOppgaveDataTilDtoMapperFraEntitetTilDto() {
        // CDI proxy
    }

    @Override
    public OppgavetypeDataDto tilDto(OppgaveDataEntitet entitet) {
        var e = (SøkYtelseOppgaveDataEntitet) entitet;
        return new SøkYtelseOppgavetypeDataDto(e.getFomDato());
    }
}

