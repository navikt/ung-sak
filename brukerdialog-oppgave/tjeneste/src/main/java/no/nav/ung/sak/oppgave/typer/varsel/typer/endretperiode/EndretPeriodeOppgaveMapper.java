package no.nav.ung.sak.oppgave.typer.varsel.typer.endretperiode;

import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.EndretPeriodeOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.PeriodeEndringType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.BrukerdialogOppgaveEntitet;
import no.nav.ung.sak.kontrakt.oppgaver.OppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.EndretPeriodeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeDTO;

import java.util.stream.Collectors;

public class EndretPeriodeOppgaveMapper {

    public static BrukerdialogOppgaveEntitet map(EndretPeriodeOppgaveDTO oppgaveDto, AktørId aktørId) {
        OppgavetypeDataDto endretPeriodeOppgaveData = new EndretPeriodeDataDto(
            mapPeriode(oppgaveDto.getNyPeriode()),
            mapPeriode(oppgaveDto.getForrigePeriode()),
            oppgaveDto.getEndringer().stream().map(EndretPeriodeOppgaveMapper::mapEndringType)
                .collect(Collectors.toSet())
        );
        return new BrukerdialogOppgaveEntitet(
            oppgaveDto.getOppgaveReferanse(),
            OppgaveType.BEKREFT_ENDRET_PERIODE,
            aktørId,
            oppgaveDto.getFrist()
        );
    }

    private static no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType mapEndringType(PeriodeEndringType it) {
        return switch (it) {
            case ENDRET_STARTDATO -> no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType.ENDRET_STARTDATO;
            case ENDRET_SLUTTDATO -> no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType.ENDRET_SLUTTDATO;
            case FJERNET_PERIODE -> no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType.FJERNET_PERIODE;
            case ANDRE_ENDRINGER -> no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType.ANDRE_ENDRINGER;
        };
    }

    private static PeriodeDTO mapPeriode(no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.PeriodeDTO periode) {
        return periode != null ? new PeriodeDTO(periode.getFom(), periode.getTom()) : null;
    }

}
