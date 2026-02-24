package no.nav.ung.sak.oppgave.typer.varsel.typer.fjernperiode;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.fjernperiode.FjernetPeriodeDataDto;
import no.nav.ung.sak.oppgave.OppgaveDataEntitetTilDtoMapper;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_FJERNET_PERIODE)
public class FjernetPeriodeOppgaveDataTilDtoMapper implements OppgaveDataEntitetTilDtoMapper {

    protected FjernetPeriodeOppgaveDataTilDtoMapper() {
        // CDI proxy
    }

    @Override
    public OppgavetypeDataDto tilDto(OppgaveDataEntitet entitet) {
        var e = (FjernetPeriodeOppgaveDataEntitet) entitet;
        return new FjernetPeriodeDataDto(e.getForrigeStartdato(), e.getForrigeSluttdato());
    }
}

