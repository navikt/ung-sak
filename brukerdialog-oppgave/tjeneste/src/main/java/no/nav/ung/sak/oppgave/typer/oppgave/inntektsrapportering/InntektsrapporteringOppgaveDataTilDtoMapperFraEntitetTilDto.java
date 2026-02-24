package no.nav.ung.sak.oppgave.typer.oppgave.inntektsrapportering;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDto;
import no.nav.ung.sak.oppgave.OppgaveDataMapperFraEntitetTilDto;
import no.nav.ung.sak.oppgave.OppgaveTypeRef;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;

@ApplicationScoped
@OppgaveTypeRef(OppgaveType.RAPPORTER_INNTEKT)
public class InntektsrapporteringOppgaveDataTilDtoMapperFraEntitetTilDto implements OppgaveDataMapperFraEntitetTilDto {

    protected InntektsrapporteringOppgaveDataTilDtoMapperFraEntitetTilDto() {
        // CDI proxy
    }

    @Override
    public OppgavetypeDataDto tilDto(OppgaveDataEntitet entitet) {
        var e = (InntektsrapporteringOppgaveDataEntitet) entitet;
        return new InntektsrapporteringOppgavetypeDataDto(
            e.getFraOgMed(),
            e.getTilOgMed(),
            e.isGjelderDelerAvMåned()
        );
    }
}

