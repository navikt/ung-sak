package no.nav.ung.sak.oppgave.typer.inntektsrapportering;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveData;
import no.nav.ung.sak.oppgave.OppgaveType;

public class InntektsrapporteringOppgaveMapper {


    public static BrukerdialogOppgaveEntitet map(InntektsrapporteringOppgaveDTO oppgaveDto, AktørId aktørId) {
        OppgaveData endretStartdatoOppgaveData = new InntektsrapporteringOppgaveData(
            oppgaveDto.getFomDato(), oppgaveDto.getTomDato(), oppgaveDto.getGjelderDelerAvMåned()
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
