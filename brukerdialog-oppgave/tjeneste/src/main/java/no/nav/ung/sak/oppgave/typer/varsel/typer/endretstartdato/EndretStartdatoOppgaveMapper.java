package no.nav.ung.sak.oppgave.typer.varsel.typer.endretstartdato;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretstartdato.EndretStartdatoDataDto;

public class EndretStartdatoOppgaveMapper {
    public static BrukerdialogOppgaveEntitet map(EndretStartdatoOppgaveDTO oppgaveDto, AktørId aktørId) {
        OppgavetypeDataDto endretStartdatoOppgaveData = new EndretStartdatoDataDto(
            oppgaveDto.getNyStartdato(), oppgaveDto.getForrigeStartdato()
        );
        BrukerdialogOppgaveEntitet nyOppgave = new BrukerdialogOppgaveEntitet(
            oppgaveDto.getOppgaveReferanse(),
            OppgaveType.BEKREFT_ENDRET_STARTDATO,
            aktørId,
            endretStartdatoOppgaveData,
            oppgaveDto.getFrist()
        );
        return nyOppgave;
    }
}

