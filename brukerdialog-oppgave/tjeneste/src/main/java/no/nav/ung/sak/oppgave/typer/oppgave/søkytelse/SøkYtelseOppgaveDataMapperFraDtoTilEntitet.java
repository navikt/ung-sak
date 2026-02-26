package no.nav.ung.sak.oppgave.typer.oppgave.søkytelse;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.søkytelse.SøkYtelseOppgavetypeDataDto;
import no.nav.ung.sak.oppgave.OppgaveDataMapperFraDtoTilEntitet;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.SØK_YTELSE)
public class SøkYtelseOppgaveDataMapperFraDtoTilEntitet implements OppgaveDataMapperFraDtoTilEntitet {

    protected SøkYtelseOppgaveDataMapperFraDtoTilEntitet() {
        // CDI proxy
    }

    @Override
    public OppgaveDataEntitet map(OppgavetypeDataDto data) {
        var dto = (SøkYtelseOppgavetypeDataDto) data;
        return new SøkYtelseOppgaveDataEntitet(dto.fomDato());
    }
}
