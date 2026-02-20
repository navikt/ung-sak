package no.nav.ung.sak.oppgave.typer.oppgave.inntektsrapportering;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDto;

public class InntektsrapporteringOppgaveMapper {


    public static BrukerdialogOppgaveEntitet map(InntektsrapporteringOppgaveDTO oppgaveDto, AktørId aktørId) {
        OppgavetypeDataDto inntektsrapporteringOppgavetypeDataDto = new InntektsrapporteringOppgavetypeDataDto(
            oppgaveDto.getFomDato(), oppgaveDto.getTomDato(), oppgaveDto.getGjelderDelerAvMåned()
        );
        BrukerdialogOppgaveEntitet nyOppgave = new BrukerdialogOppgaveEntitet(
            oppgaveDto.getReferanse(),
            OppgaveType.RAPPORTER_INNTEKT,
            aktørId,
            inntektsrapporteringOppgavetypeDataDto,
            oppgaveDto.getFrist()
        );
        return nyOppgave;
    }
}
