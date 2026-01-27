package no.nav.ung.sak.oppgave.endretsluttdato;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveData;
import no.nav.ung.sak.oppgave.OppgaveType;

public class EndretSluttdatoOppgaveMapper {
    public static BrukerdialogOppgaveEntitet map(EndretSluttdatoOppgaveDTO oppgaveDto, AktørId aktørId) {
        OppgaveData endretSlutttdatoOppgaveData = new EndretSluttdatoOppgaveData(
            oppgaveDto.getNySluttdato(), oppgaveDto.getForrigeSluttdato()
        );
        BrukerdialogOppgaveEntitet nyOppgave = new BrukerdialogOppgaveEntitet(
            oppgaveDto.getOppgaveReferanse(),
            OppgaveType.BEKREFT_ENDRET_SLUTTDATO,
            aktørId,
            endretSlutttdatoOppgaveData,
            oppgaveDto.getFrist()
        );
        return nyOppgave;
    }
}
