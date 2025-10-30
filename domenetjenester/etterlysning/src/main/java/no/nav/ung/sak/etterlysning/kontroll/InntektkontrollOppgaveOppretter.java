package no.nav.ung.sak.etterlysning.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.kontroll.RapportertInntektMapper;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.util.List;
import java.util.function.Function;

@Dependent
public class InntektkontrollOppgaveOppretter {

    private final UngOppgaveKlient ungOppgaveKlient;
    private final RapportertInntektMapper rapportertInntektMapper;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @Inject
    public InntektkontrollOppgaveOppretter(UngOppgaveKlient ungOppgaveKlient, RapportertInntektMapper rapportertInntektMapper, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        LocalDateTimeline<Boolean> programTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId());
        var oppgaveDtoer = etterlysninger.stream().map(mapTilDto(behandling.getId(), deltakerIdent, programTidslinje)).toList();
        oppgaveDtoer.forEach(ungOppgaveKlient::opprettKontrollerRegisterInntektOppgave);
    }

    private Function<Etterlysning, RegisterInntektOppgaveDTO> mapTilDto(long behandlingId, PersonIdent deltakerIdent, LocalDateTimeline<Boolean> programTidslinje) {
        return etterlysning -> {
            var registerinntekter = rapportertInntektMapper.finnRegisterinntekterForPeriodeOgGrunnlag(behandlingId, etterlysning.getGrunnlagsreferanse(), etterlysning.getPeriode().toLocalDateInterval());
            boolean gjelderSistePeriode = programTidslinje.intersection(new LocalDateInterval(etterlysning.getPeriode().getTomDato().plusDays(1), etterlysning.getPeriode().getTomDato().plusDays(1))).isEmpty();
            return new RegisterInntektOppgaveDTO(deltakerIdent.getIdent(),
                etterlysning.getEksternReferanse(),
                etterlysning.getFrist(),
                etterlysning.getPeriode().getFomDato(),
                etterlysning.getPeriode().getTomDato(),
                InntektKontrollOppgaveMapper.mapTilRegisterInntekter(registerinntekter),
                gjelderSistePeriode
                );
        };
    }


}
