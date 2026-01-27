package no.nav.ung.sak.oppgave.typer.endretstartdato;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.oppgave.OppgaveData;
import no.nav.ung.sak.oppgave.OppgaveType;

public class EndretStartdatoOppgaveMapper {
    public static BrukerdialogOppgaveEntitet map(EndretStartdatoOppgaveDTO oppgaveDto, AktørId aktørId) {
        OppgaveData endretStartdatoOppgaveData = new EndretStartdatoOppgaveData(
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

