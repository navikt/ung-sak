package no.nav.ung.sak.oppgave.typer.inntektsrapportering;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.kontrakt.OppgavetypeDataDTO;
import no.nav.ung.sak.oppgave.kontrakt.OppgaveType;
import no.nav.ung.sak.oppgave.kontrakt.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDTO;

public class InntektsrapporteringOppgaveMapper {


    public static BrukerdialogOppgaveEntitet map(InntektsrapporteringOppgaveDTO oppgaveDto, AktørId aktørId) {
        OppgavetypeDataDTO endretStartdatoOppgaveData = new InntektsrapporteringOppgavetypeDataDTO(
            oppgaveDto.getFomDato(), oppgaveDto.getTomDato(), null, oppgaveDto.getGjelderDelerAvMåned()
        );
        BrukerdialogOppgaveEntitet nyOppgave = new BrukerdialogOppgaveEntitet(
            oppgaveDto.getReferanse(),
            OppgaveType.RAPPORTER_INNTEKT,
            aktørId,
            endretStartdatoOppgaveData,
            oppgaveDto.getFrist()
        );
        return nyOppgave;
    }
}
