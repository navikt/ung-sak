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
import java.util.List;
import java.util.Set;

public class EndringBarnetilleggScenarioer {
    /**
     * Endring barnetillegg. Får barn etter å ha fått innvilget programmet
     */
    public static UngTestScenario endringBarnetillegg(LocalDate fom, LocalDate barnFødselsdato) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, barnFødselsdato.minusDays(1), BrevScenarioerUtils.lavSatsBuilder(fom).build()),
            new LocalDateSegment<>(barnFødselsdato, p.getTomDato(), BrevScenarioerUtils.lavSatsMedBarnBuilder(barnFødselsdato, 1).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(p),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fra(barnFødselsdato, p.getTomDato()))),
                List.of(
                BrevScenarioerUtils.lagBarn(barnFødselsdato)
            ), null);
    }

    /**
     * Endring barnetillegg. Har allerede ett barn og får tvillinger
     */
    public static UngTestScenario endringBarnetilleggFlereBarn(LocalDate fom, LocalDate barnFødselsdato) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, barnFødselsdato.minusDays(1), BrevScenarioerUtils.lavSatsMedBarnBuilder(fom, 1).build()),
            new LocalDateSegment<>(barnFødselsdato, p.getTomDato(), BrevScenarioerUtils.lavSatsMedBarnBuilder(barnFødselsdato, 3).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(p),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fra(barnFødselsdato, p.getTomDato()))),
                List.of(
                BrevScenarioerUtils.lagBarn(barnFødselsdato.minusYears(5)),
                BrevScenarioerUtils.lagBarn(barnFødselsdato),
                BrevScenarioerUtils.lagBarn(barnFødselsdato)
            ), null);
    }

    /**
     * Endring pga dødsfall av barn.
     */
    public static UngTestScenario endringDødsfall(LocalDate fom, LocalDate barnDødsdato) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, barnDødsdato.minusDays(1), BrevScenarioerUtils.lavSatsMedBarnBuilder(fom, 1).build()),
            new LocalDateSegment<>(barnDødsdato, p.getTomDato(), BrevScenarioerUtils.lavSatsMedBarnBuilder(barnDødsdato, 0).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(p),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN, DatoIntervallEntitet.fra(barnDødsdato, p.getTomDato()))),
                List.of(
                BrevScenarioerUtils.lagBarnMedDødsdato(fom.minusYears(1), barnDødsdato)
            ), null);
    }
}
