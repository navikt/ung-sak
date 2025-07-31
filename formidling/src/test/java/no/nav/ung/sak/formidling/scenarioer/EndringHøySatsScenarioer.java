package no.nav.ung.sak.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.trigger.Trigger;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EndringHøySatsScenarioer {
    /**
     * 24 år blir 25 år etter 3 mnd i progrmmet og får overgang til høy sats
     */
    public static UngTestScenario endring25År(LocalDate fødselsdato) {
        var tjuvefemårsdag = fødselsdato.plusYears(25);
        var fom = tjuvefemårsdag.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(3);

        var programPeriode = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(programPeriode.getFomDato(), tjuvefemårsdag.minusDays(1), BrevScenarioerUtils.lavSatsBuilder(fom).build()),
            new LocalDateSegment<>(tjuvefemårsdag, programPeriode.getTomDato(), BrevScenarioerUtils.høySatsBuilder(fom).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(programPeriode.getFomDato(), programPeriode.getTomDato()));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(programPeriode),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            fødselsdato,
            List.of(programPeriode.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fra(tjuvefemårsdag, programPeriode.getTomDato()))), null, Collections.emptyList(), null);
    }

    /**
     * 24 år blir 25 år etter 3 mnd i progrmmet og får overgang til høy sats. Har barn fra før av
     */
    public static UngTestScenario endring25ÅrMedToBarn(LocalDate fødselsdato) {
        var tjuvefemårsdag = fødselsdato.plusYears(25);
        var fom = tjuvefemårsdag.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(3);

        var programPeriode = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(programPeriode.getFomDato(), tjuvefemårsdag.minusDays(1), BrevScenarioerUtils.lavSatsMedBarnBuilder(fom, 2).build()),
            new LocalDateSegment<>(tjuvefemårsdag, programPeriode.getTomDato(), BrevScenarioerUtils.høySatsBuilderMedBarn(tjuvefemårsdag, 2).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(programPeriode.getFomDato(), programPeriode.getTomDato()));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(programPeriode),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            fødselsdato,
            List.of(programPeriode.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fra(tjuvefemårsdag, programPeriode.getTomDato()))),
            null,
            List.of(
                BrevScenarioerUtils.lagBarn(fom.minusYears(5))
            ), null);
    }
}
