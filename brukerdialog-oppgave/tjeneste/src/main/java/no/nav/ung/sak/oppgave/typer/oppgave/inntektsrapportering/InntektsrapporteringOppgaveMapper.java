package no.nav.ung.sak.oppgave.typer.oppgave.inntektsrapportering;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDTO;

public class InntektsrapporteringOppgaveMapper {


    public static BrukerdialogOppgaveEntitet map(InntektsrapporteringOppgaveDTO oppgaveDto, AktørId aktørId) {
        OppgavetypeDataDTO inntektsrapporteringOppgavetypeDataDTO = new InntektsrapporteringOppgavetypeDataDTO(
            oppgaveDto.getFomDato(), oppgaveDto.getTomDato(), oppgaveDto.getGjelderDelerAvMåned()
        );
        BrukerdialogOppgaveEntitet nyOppgave = new BrukerdialogOppgaveEntitet(
            oppgaveDto.getReferanse(),
            OppgaveType.RAPPORTER_INNTEKT,
            aktørId,
            inntektsrapporteringOppgavetypeDataDTO,
            oppgaveDto.getFrist()
        );
        return nyOppgave;
    }
}
