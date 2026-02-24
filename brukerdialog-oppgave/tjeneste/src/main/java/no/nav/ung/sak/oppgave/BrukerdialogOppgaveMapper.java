package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.sak.kontrakt.oppgaver.BrukerdialogOppgaveDto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@ApplicationScoped
public class BrukerdialogOppgaveMapper {

    private Instance<OppgaveDataEntitetTilDtoMapper> mappere;

    public BrukerdialogOppgaveMapper() {
        // CDI proxy
    }

    @Inject
    public BrukerdialogOppgaveMapper(@Any Instance<OppgaveDataEntitetTilDtoMapper> mappere) {
        this.mappere = mappere;
    }

    public BrukerdialogOppgaveDto tilDto(BrukerdialogOppgaveEntitet oppgave) {
        var oppgavetypeData = OppgaveDataEntitetTilDtoMapper.finnTjeneste(mappere, oppgave.getOppgaveType())
            .tilDto(oppgave.getOppgaveData());

        return new BrukerdialogOppgaveDto(
            oppgave.getOppgavereferanse(),
            oppgave.getOppgaveType(),
            oppgavetypeData,
            oppgave.getBekreftelse(),
            oppgave.getStatus(),
            toZonedDateTime(oppgave.getOpprettetTidspunkt()),
            toZonedDateTime(oppgave.getLøstDato()),
            toZonedDateTime(oppgave.getÅpnetDato()),
            toZonedDateTime(oppgave.getLukketDato()),
            toZonedDateTime(oppgave.getFristTid())
        );
    }


    private ZonedDateTime toZonedDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault());
    }
}

