package no.nav.ung.sak.etterlysning.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.etterlysning.MidlertidigOppgaveDelegeringTjeneste;
import no.nav.ung.sak.kontroll.RapportertInntektMapper;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgavetypeDataDto;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.util.List;
import java.util.function.Function;

@Dependent
public class InntektkontrollOppgaveOppretter {

    private final MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste;
    private final RapportertInntektMapper rapportertInntektMapper;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @Inject
    public InntektkontrollOppgaveOppretter(MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste, RapportertInntektMapper rapportertInntektMapper, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.delegeringTjeneste = delegeringTjeneste;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        LocalDateTimeline<Boolean> programTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId());
        etterlysninger.stream()
            .map(mapTilDto(behandling.getId(), deltakerIdent, programTidslinje))
            .forEach(delegeringTjeneste::opprettOppgave);
    }

    private Function<Etterlysning, OpprettOppgaveDto> mapTilDto(long behandlingId, PersonIdent deltakerIdent, LocalDateTimeline<Boolean> programTidslinje) {
        return etterlysning -> {
            var registerinntekter = rapportertInntektMapper.finnRegisterinntekterForPeriodeOgGrunnlag(behandlingId, etterlysning.getGrunnlagsreferanse(), etterlysning.getPeriode().toLocalDateInterval());
            LocalDateInterval etterlysningPeriode = etterlysning.getPeriode().toLocalDateInterval();
            return new OpprettOppgaveDto(
                deltakerIdent.getIdent(),
                etterlysning.getEksternReferanse(),
                new KontrollerRegisterinntektOppgavetypeDataDto(
                    etterlysning.getPeriode().getFomDato(),
                    etterlysning.getPeriode().getTomDato(),
                    InntektKontrollOppgaveMapper.mapTilRegisterInntekter(registerinntekter),
                    overlapperPeriodeDelvisMedProgramtidslinje(etterlysningPeriode, programTidslinje)
                ),
                etterlysning.getFrist()
            );
        };
    }

    private static <T> boolean overlapperPeriodeDelvisMedProgramtidslinje(LocalDateInterval periode, LocalDateTimeline<T> programtidslinje) {
        LocalDateTimeline<Boolean> periodeSomTidslinje = new LocalDateTimeline<>(periode, true);
        LocalDateTimeline<T> overlapp = programtidslinje.intersection(periode);
        LocalDateTimeline<Boolean> periodeEtterFjernetOverlapp = periodeSomTidslinje.disjoint(overlapp);
        return !periodeEtterFjernetOverlapp.isEmpty();
    }


}
