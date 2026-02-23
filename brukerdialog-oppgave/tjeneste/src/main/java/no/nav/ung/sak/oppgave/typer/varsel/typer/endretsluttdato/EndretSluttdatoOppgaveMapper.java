package no.nav.ung.sak.oppgave.typer.varsel.typer.endretsluttdato;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato.EndretSluttdatoDataDto;

public class EndretSluttdatoOppgaveMapper {
    public static BrukerdialogOppgaveEntitet map(EndretSluttdatoOppgaveDTO oppgaveDto, AktørId aktørId) {
        OppgavetypeDataDto endretSlutttdatoOppgaveData = new EndretSluttdatoDataDto(
            oppgaveDto.getNySluttdato(), oppgaveDto.getForrigeSluttdato()
        );
        return new BrukerdialogOppgaveEntitet(
            oppgaveDto.getOppgaveReferanse(),
            OppgaveType.BEKREFT_ENDRET_SLUTTDATO,
            aktørId,
            oppgaveDto.getFrist()
        );
    }
}
