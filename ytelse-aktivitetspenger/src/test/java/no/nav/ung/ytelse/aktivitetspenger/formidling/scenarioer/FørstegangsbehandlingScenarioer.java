package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestRepositories;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenario;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FørstegangsbehandlingScenarioer {

    public static AktivitetspengerTestScenarioBuilder lagAvsluttetStandardBehandling(AktivitetspengerTestRepositories repositories) {
        AktivitetspengerTestScenario testscenario = innvilget19årLavInntekt(LocalDate.of(2024, 12, 1));

        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(testscenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return scenarioBuilder;
    }

    /**
     * 19 år med lav sats, inntekt lavere enn minsteytelse (bruker minsteytelse)
     */
    public static AktivitetspengerTestScenario innvilget19årLavInntekt(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satsGrunnlag = BrevScenarioerUtils.lavSatsGrunnlag(fom);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedLavInntekt(fom);
        var satser = BrevScenarioerUtils.lagAktivitetspengerSatser(satsGrunnlag, beregningsgrunnlag);

        return new AktivitetspengerTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new Periode(fom, fom.plusYears(1))),
            BrevScenarioerUtils.tilkjentYtelsePerioder(new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1)), satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_AKTIVITETSPENGER_PERIODE, DatoIntervallEntitet.fra(p))),
            Collections.emptyList(),
            null,
            null);
    }

    /**
     * 19 år med lav sats, inntekt høyere enn minsteytelse (bruker beregningsgrunnlag)
     */
    public static AktivitetspengerTestScenario innvilget19årHøyInntekt(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satsGrunnlag = BrevScenarioerUtils.lavSatsGrunnlag(fom);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedHøyInntekt(fom);
        var satser = BrevScenarioerUtils.lagAktivitetspengerSatser(satsGrunnlag, beregningsgrunnlag);

        return new AktivitetspengerTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new Periode(fom, fom.plusYears(1))),
            BrevScenarioerUtils.tilkjentYtelsePerioder(new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1)), satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_AKTIVITETSPENGER_PERIODE, DatoIntervallEntitet.fra(p))),
            Collections.emptyList(),
            null,
            null);
    }

    /**
     * 19 år med 1 barn 15 dager etter fom, lav sats, lav inntekt
     */
    public static AktivitetspengerTestScenario innvilget19årMedBarn15DagerEtterStartdato(LocalDate fom) {
        LocalDate barnFødselsdato = fom.plusDays(15);
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satsGrunnlag = BrevScenarioerUtils.lavSatsGrunnlagMedBarn(fom, 1);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedLavInntekt(fom);
        var satser = BrevScenarioerUtils.lagAktivitetspengerSatser(satsGrunnlag, beregningsgrunnlag);

        return new AktivitetspengerTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new Periode(fom, fom.plusYears(1))),
            BrevScenarioerUtils.tilkjentYtelsePerioder(new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1)), satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_AKTIVITETSPENGER_PERIODE, DatoIntervallEntitet.fra(p))),
            List.of(BrevScenarioerUtils.lagBarn(barnFødselsdato)),
            null,
            null);
    }

    /**
     * 27 år med høy sats, lav inntekt (bruker minsteytelse)
     */
    public static AktivitetspengerTestScenario innvilget27årHøySats(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var satsGrunnlag = BrevScenarioerUtils.høySatsGrunnlag(fom);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedLavInntekt(fom);
        var satser = BrevScenarioerUtils.lagAktivitetspengerSatser(satsGrunnlag, beregningsgrunnlag);

        return new AktivitetspengerTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new Periode(fom, fom.plusWeeks(52).minusDays(1))),
            BrevScenarioerUtils.tilkjentYtelsePerioder(new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1)), satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(27).plusDays(42),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_AKTIVITETSPENGER_PERIODE, DatoIntervallEntitet.fra(p))),
            Collections.emptyList(),
            null,
            null);
    }

    /**
     * 27 år med høy sats, høy inntekt (bruker beregningsgrunnlag)
     */
    public static AktivitetspengerTestScenario innvilget27årHøySatsHøyInntekt(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var satsGrunnlag = BrevScenarioerUtils.høySatsGrunnlag(fom);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedHøyInntekt(fom);
        var satser = BrevScenarioerUtils.lagAktivitetspengerSatser(satsGrunnlag, beregningsgrunnlag);

        return new AktivitetspengerTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new Periode(fom, fom.plusWeeks(52).minusDays(1))),
            BrevScenarioerUtils.tilkjentYtelsePerioder(new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1)), satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(27).plusDays(42),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_AKTIVITETSPENGER_PERIODE, DatoIntervallEntitet.fra(p))),
            Collections.emptyList(),
            null,
            null);
    }

    /**
     * 24 år blir 25 år i programmet. Får overgang fra lav til høy sats.
     * Med 2 barn. Lav inntekt.
     */
    public static AktivitetspengerTestScenario innvilget24årBle25MedBarn(LocalDate fom) {
        LocalDate barnFødselsdato = fom.plusDays(15);
        LocalDate tjuefemårsdato = barnFødselsdato.plusDays(2);
        LocalDate fødselsdato = tjuefemårsdato.minusYears(25);
        var p = new LocalDateInterval(fom, fom.plusYears(1));

        var satsGrunnlagLav = BrevScenarioerUtils.lavSatsGrunnlagMedBarn(fom, 2);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedLavInntekt(fom);
        var satserLav = BrevScenarioerUtils.lagAktivitetspengerSatser(satsGrunnlagLav, beregningsgrunnlag);

        return new AktivitetspengerTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new Periode(fom, fom.plusYears(1))),
            BrevScenarioerUtils.tilkjentYtelsePerioder(new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1)), satserLav),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_AKTIVITETSPENGER_PERIODE, DatoIntervallEntitet.fra(p))),
            List.of(
                BrevScenarioerUtils.lagBarn(barnFødselsdato),
                BrevScenarioerUtils.lagBarn(barnFødselsdato)
            ),
            null,
            null);
    }

    /**
     * Delvis innvilget - 18 år blir 19 år i program
     */
    public static AktivitetspengerTestScenario innvilgetDelvis(LocalDate fom, LocalDate nittenårsdag) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        LocalDate fødselsdato = nittenårsdag.minusYears(19);
        var satsGrunnlag = BrevScenarioerUtils.lavSatsGrunnlag(fom);
        var beregningsgrunnlag = BrevScenarioerUtils.lagBeregningsgrunnlagMedLavInntekt(nittenårsdag);
        var satser = BrevScenarioerUtils.lagAktivitetspengerSatser(satsGrunnlag, beregningsgrunnlag);

        return new AktivitetspengerTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new Periode(fom, fom.plusYears(1))),
            BrevScenarioerUtils.tilkjentYtelsePerioder(new LocalDateInterval(nittenårsdag, fom.plusMonths(1).minusDays(1)), satser),
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, nittenårsdag.minusDays(1), Utfall.IKKE_OPPFYLT),
                new LocalDateSegment<>(nittenårsdag, p.getTomDato(), Utfall.OPPFYLT)
            )),
            fødselsdato,
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_AKTIVITETSPENGER_PERIODE, DatoIntervallEntitet.fra(p))),
            Collections.emptyList(),
            null,
            null);
    }

    /**
     * Manuelt endret sats med RE_SATS_ENDRING
     */
    public static AktivitetspengerTestScenario endretSats(LocalDate fom) {
        AktivitetspengerTestScenario scenario = innvilget19årLavInntekt(fom);
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        return new AktivitetspengerTestScenario(
            scenario.navn(),
            scenario.søknadsperioder(),
            scenario.tilkjentYtelsePerioder(),
            scenario.aldersvilkår(),
            scenario.fødselsdato(),
            Set.of(new Trigger(BehandlingÅrsakType.RE_SATS_ENDRING, DatoIntervallEntitet.fra(p))),
            scenario.barn(),
            scenario.dødsdato(),
            null);
    }
}
