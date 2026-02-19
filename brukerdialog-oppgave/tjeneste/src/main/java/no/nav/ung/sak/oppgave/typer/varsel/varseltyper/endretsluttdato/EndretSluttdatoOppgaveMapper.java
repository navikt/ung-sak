package no.nav.ung.sak.oppgave.typer.varsel.varseltyper.endretsluttdato;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDTO;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato.EndretSluttdatoDataDTO;

public class EndretSluttdatoOppgaveMapper {
    public static BrukerdialogOppgaveEntitet map(EndretSluttdatoOppgaveDTO oppgaveDto, AktørId aktørId) {
        OppgavetypeDataDTO endretSlutttdatoOppgaveData = new EndretSluttdatoDataDTO(
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
