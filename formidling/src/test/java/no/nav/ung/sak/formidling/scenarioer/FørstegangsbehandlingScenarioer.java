package no.nav.ung.sak.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.uttak.Tid;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.trigger.Trigger;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FørstegangsbehandlingScenarioer {
    public static TestScenarioBuilder lagAvsluttetStandardBehandling(UngTestRepositories repositories) {
        UngTestScenario ungTestscenario = innvilget19år(LocalDate.of(2024, 12, 1));

        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestscenario);

        var behandling = scenarioBuilder.buildOgLagreMedUng(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return scenarioBuilder;
    }

    /**
     * 19 år ungdom med full ungdomsperiode, ingen inntektsgradering og ingen barn
     */
    public static UngTestScenario innvilget19år(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));

        var satser = new LocalDateTimeline<>(p, BrevScenarioerUtils.lavSatsBuilder(fom).build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), Tid.TIDENES_ENDE));

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
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null,
            Collections.emptyList(), null);
    }

    /**
     * 19 år ungdom med full ungdomsperiode med 1 barn 15 dager etter fom, ingen inntektsgradering
     */
    public static UngTestScenario innvilget19årMedBarn15DagerEtterStartdato(LocalDate fom) {
        LocalDate barnFødselsdato = fom.plusDays(15);
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(p.getFomDato(), barnFødselsdato.minusDays(1), BrevScenarioerUtils.lavSatsBuilder(fom).build()),
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
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null,
            List.of(
                BrevScenarioerUtils.lagBarn(barnFødselsdato)
            ), null);
    }

    /**
     * Førstegangsbehandling med dødsfall av barn
     */
    public static UngTestScenario innvilget19årMedDødsfallBarn15DagerEtterStartdato(LocalDate fom) {
        LocalDate barnDødsdato = fom.plusDays(15);
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(p.getFomDato(), barnDødsdato.minusDays(1), BrevScenarioerUtils.lavSatsMedBarnBuilder(fom, 1).build()),
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
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null,
            List.of(
                BrevScenarioerUtils.lagBarnMedDødsdato(fom.minusYears(1), barnDødsdato)
            ), null);
    }

    /**
     * Scenario med alle kombinasjoner utenom dødsfall av barn:
     * Innvilget fom lenge før dagens dato
     * 24 år ungdom blir 25 år i mai. 2 barn født etter startdato . Overgang av G-beløp i tillegg
     * Søker i mai slutten av mai med startdato 20 april. Får hele mai og april
     * Får 2 barn så overgang til 25 år.
     */
    public static UngTestScenario innvilget24MedAlleKombinasjonerFom21April2025() {
        LocalDate fom = LocalDate.of(2025, 4, 21);
        LocalDate barnFødselsdato = fom.plusDays(15);
        LocalDate tjuvefemårsdato = barnFødselsdato.plusDays(2);
        LocalDate fødselsdato = tjuvefemårsdato.minusYears(25);

        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(p.getFomDato(), barnFødselsdato.minusDays(1), BrevScenarioerUtils.lavSatsBuilder(fom).build()),
            new LocalDateSegment<>(barnFødselsdato, tjuvefemårsdato.minusDays(1), BrevScenarioerUtils.lavSatsMedBarnBuilder(barnFødselsdato, 2).build()), //Får ny G
            new LocalDateSegment<>(tjuvefemårsdato, p.getTomDato(), BrevScenarioerUtils.høySatsBuilderMedBarn(barnFødselsdato, 2).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(p),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null,
            List.of(
                BrevScenarioerUtils.lagBarn(barnFødselsdato),
                BrevScenarioerUtils.lagBarn(barnFødselsdato)
            ),
            null);
    }

    /**
     * 27 år ungdom med full ungdomsperiode, ingen inntektsgradering og ingen barn, høy sats
     */
    public static UngTestScenario innvilget27år(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(p,
            BrevScenarioerUtils.høySatsBuilder(fom).build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(p),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(27).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null, Collections.emptyList(), null);
    }

    /**
     * 28 år ungdom med ungdomsprogramperiode fram til 29 år,
     * ingen inntektsgradering og ingen barn, høy sats
     */
    public static UngTestScenario innvilget29År(LocalDate fom, LocalDate fødselsdato) {
        var p = new LocalDateInterval(fom, fødselsdato.plusYears(29));

        var satser = new LocalDateTimeline<>(p, BrevScenarioerUtils.høySatsBuilder(fom).build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(p),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null, Collections.emptyList(), null);
    }

    /**
     * Søker bakover i tid med startdato lik første i måneden fylte 25 år.
     * Blir 25 etter 15 dager i programmet
     * Søker måneden etter fylte 25 år.
     * Så er 24 år ved startdato.
     * Får både lav og høy sats i førstegangsbehandlingen
     * ingen inntektsgradering og ingen barn
     */
    public static UngTestScenario innvilget24årBle25årførsteMåned(LocalDate fom) {
        LocalDate tjuvefemårsdag = fom.plusDays(15);
        LocalDate fødselsdato = tjuvefemårsdag.minusYears(25);
        LocalDate tom25årmnd = tjuvefemårsdag.with(TemporalAdjusters.lastDayOfMonth());
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), BrevScenarioerUtils.lavSatsBuilder(p.getFomDato()).build()),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), BrevScenarioerUtils.høySatsBuilder(tjuvefemårsdag).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom, tom25årmnd);
        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(p),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, tilkjentPeriode),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null, Collections.emptyList(), null);
    }

    /**
     *
     *  Innvilget med sluttdato
     *
     */
    public static UngTestScenario innvilget19årMedSluttdato(LocalDate startdato, LocalDate sluttdato) {
        var p = new LocalDateInterval(startdato, startdato.plusYears(1));
        var fagsakPeriode = new LocalDateInterval(startdato, startdato.plusWeeks(52).minusDays(1));
        var programperiode = new LocalDateInterval(startdato, sluttdato);
        var satser = new LocalDateTimeline<>(p, BrevScenarioerUtils.lavSatsBuilder(startdato).build());

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(programperiode.getFomDato(), programperiode.getTomDato())),
            satser,
            BrevScenarioerUtils.uttaksPerioder(programperiode),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, programperiode),
            new LocalDateTimeline<>(fagsakPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(programperiode, Utfall.OPPFYLT),
                new LocalDateSegment<>(sluttdato.plusDays(1), fagsakPeriode.getTomDato(), Utfall.IKKE_OPPFYLT)
            )

            ),
            startdato.minusYears(19).plusDays(42),
            List.of(startdato),
            Set.of(
                new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(startdato, sluttdato)),
                new Trigger(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER, DatoIntervallEntitet.fra(startdato, sluttdato))
            ),
            null,
            Collections.emptyList(),
            null);
    }

    /**
     * 18 år blir 19 år i program. Usikker om dette er mulig
     */
    public static UngTestScenario innvilgetDelvis(LocalDate fom, LocalDate nittenårsdag) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));

        var satser = new LocalDateTimeline<>(p, BrevScenarioerUtils.lavSatsBuilder(fom).build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), Tid.TIDENES_ENDE));

        LocalDate fødselsdato = nittenårsdag.minusYears(19);

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(p),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, nittenårsdag.minusDays(1), Utfall.IKKE_OPPFYLT),
                new LocalDateSegment<>(nittenårsdag, p.getTomDato(), Utfall.OPPFYLT)
            )),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato, // 10 dager til blir 19 år
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null,
            Collections.emptyList(), null);
    }
}
