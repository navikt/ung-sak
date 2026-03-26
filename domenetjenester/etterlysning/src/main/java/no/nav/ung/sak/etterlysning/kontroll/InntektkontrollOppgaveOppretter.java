package no.nav.ung.sak.etterlysning.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgavetypeDataDto;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.ung.sak.etterlysning.MidlertidigOppgaveDelegeringTjeneste;
import no.nav.ung.sak.etterlysning.OppgaveYtelsetypeMapper;
import no.nav.ung.sak.kontroll.InntekterForKilde;
import no.nav.ung.sak.kontroll.RapportertInntektMapper;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.ytelseperioder.KvalifiserteYtelsesperioderTjeneste;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Dependent
public class InntektkontrollOppgaveOppretter {

    private final MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste;
    private final RapportertInntektMapper rapportertInntektMapper;
    private final Instance<KvalifiserteYtelsesperioderTjeneste> periodeTjenester;
    private final ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    @Inject
    public InntektkontrollOppgaveOppretter(MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste, RapportertInntektMapper rapportertInntektMapper,
                                           @Any Instance<KvalifiserteYtelsesperioderTjeneste> periodeTjenester, ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.delegeringTjeneste = delegeringTjeneste;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.periodeTjenester = periodeTjenester;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        LocalDateTimeline<Boolean> programTidslinje = KvalifiserteYtelsesperioderTjeneste.finnTjeneste(behandling.getFagsakYtelseType(), periodeTjenester).finnPeriodeTidslinje(behandling.getId());
        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());
        etterlysninger.stream()
            .map(mapTilDto(behandling.getId(), aktørId, programTidslinje, ytelsetype))
            .forEach(delegeringTjeneste::opprettOppgave);
    }

    private Function<Etterlysning, OpprettOppgaveDto> mapTilDto(long behandlingId, AktørId aktørId, LocalDateTimeline<Boolean> programTidslinje, OppgaveYtelsetype ytelsetype) {
        return etterlysning -> {
            var registerinntekter = rapportertInntektMapper.finnRegisterinntekterForPeriodeOgGrunnlag(behandlingId, etterlysning.getGrunnlagsreferanse(), etterlysning.getPeriode().toLocalDateInterval());
            List<ArbeidsgiverOpplysninger> arbeidsgiverOpplysninger = registerinntekter.stream().map(InntekterForKilde::arbeidsgiver)
                .distinct()
                .map(arbeidsgiverTjeneste::hent)
                .collect(Collectors.toList());
            LocalDateInterval etterlysningPeriode = etterlysning.getPeriode().toLocalDateInterval();
            return new OpprettOppgaveDto(
                new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
                ytelsetype,
                etterlysning.getEksternReferanse(),
                new KontrollerRegisterinntektOppgavetypeDataDto(
                    etterlysning.getPeriode().getFomDato(),
                    etterlysning.getPeriode().getTomDato(),
                    InntektKontrollOppgaveMapper.mapTilRegisterInntekter(registerinntekter, arbeidsgiverOpplysninger),
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
