package no.nav.ung.sak.oppgave.typer.varsel.typer.endretperiode;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.EndretPeriodeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeDTO;
import no.nav.ung.sak.oppgave.OppgaveDataMapperFraEntitetTilDto;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.BEKREFT_ENDRET_PERIODE)
public class EndretPeriodeOppgaveDataTilDtoMapperFraEntitetTilDto implements OppgaveDataMapperFraEntitetTilDto {

    protected EndretPeriodeOppgaveDataTilDtoMapperFraEntitetTilDto() {
        // CDI proxy
    }

    @Override
    public OppgavetypeDataDto tilDto(OppgaveDataEntitet entitet) {
        var e = (EndretPeriodeOppgaveDataEntitet) entitet;

        PeriodeDTO nyPeriode = e.getNyPeriodeFom() != null
            ? new PeriodeDTO(e.getNyPeriodeFom(), e.getNyPeriodeTom())
            : null;

        PeriodeDTO forrigePeriode = e.getForrigePeriodeFom() != null
            ? new PeriodeDTO(e.getForrigePeriodeFom(), e.getForrigePeriodeTom())
            : null;

        return new EndretPeriodeDataDto(nyPeriode, forrigePeriode, e.getEndringer());
    }
}

