package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Beløp;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BeregningInput;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils.*;

public class AktivitetspengerEndringHøySatsScenarioer {

    /**
     * Overgang fra lav til høy sats (minsteytelse).
     * Bruker fyller 25 år 16 dager etter fom.
     */
    public static AktivitetspengerTestScenario lavTilHøySats(LocalDate fom) {
        LocalDate tjuvefemårsdag = fom.plusDays(15);
        LocalDate fødselsdato = tjuvefemårsdag.minusYears(25);
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var lavSats = lavSatsBuilder(fom).build();
        var høySats = høySatsBuilder(tjuvefemårsdag).build();

        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), new AktivitetspengerSatsPeriode(new LocalDateInterval(fom, tjuvefemårsdag.minusDays(1)), lavSats)),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), new AktivitetspengerSatsPeriode(new LocalDateInterval(tjuvefemårsdag, p.getTomDato()), høySats))
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), lavSats),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), høySats)
        ));

        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagBeregningsgrunnlag(fom))
        ));

        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom, tjuvefemårsdag.with(TemporalAdjusters.lastDayOfMonth()));

        return new AktivitetspengerTestScenario(
            DEFAULT_NAVN,
            List.of(new Periode(fom, p.getTomDato())),
            satsperioder,
            beregningsgrunnlag,
            tilkjentYtelsePerioder(lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag), tilkjentPeriode),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            Set.of(new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fra(tjuvefemårsdag, p.getTomDato()))),
            Collections.emptyList(),
            null,
            null);
    }

    /**
     * Overgang fra beregningsgrunnlag (lav aldersgruppe) til høy sats (minsteytelse).
     * Beregningsgrunnlag gir høyere dagsats enn minstesats for lav sats,
     * men for høy sats er minstesatsen høyere enn beregningsgrunnlaget.
     */
    public static AktivitetspengerTestScenario beregningsgrunnlagTilHøySats(LocalDate fom) {
        LocalDate tjuvefemårsdag = fom.plusDays(15);
        LocalDate fødselsdato = tjuvefemårsdag.minusYears(25);
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var lavSats = lavSatsBuilder(fom).build();
        var høySats = høySatsBuilder(tjuvefemårsdag).build();

        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), new AktivitetspengerSatsPeriode(new LocalDateInterval(fom, tjuvefemårsdag.minusDays(1)), lavSats)),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), new AktivitetspengerSatsPeriode(new LocalDateInterval(tjuvefemårsdag, p.getTomDato()), høySats))
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), lavSats),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), høySats)
        ));

        // Beregningsgrunnlag som er høyere enn lav minstesats men lavere enn høy minstesats
        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagHøytBeregningsgrunnlag(fom))
        ));

        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom, tjuvefemårsdag.with(TemporalAdjusters.lastDayOfMonth()));

        return new AktivitetspengerTestScenario(
            DEFAULT_NAVN,
            List.of(new Periode(fom, p.getTomDato())),
            satsperioder,
            beregningsgrunnlag,
            tilkjentYtelsePerioder(lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag), tilkjentPeriode),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            Set.of(new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fra(tjuvefemårsdag, p.getTomDato()))),
            Collections.emptyList(),
            null,
            null);
    }

    /**
     * Alle perioder har beregningsgrunnlag som gir høyere dagsats enn minstesatsen, også etter 25 år.
     * I dette tilfellet skal det IKKE sendes brev.
     */
    public static AktivitetspengerTestScenario beregningsgrunnlagHøyereEnnSatsForAllePerioder(LocalDate fom) {
        LocalDate tjuvefemårsdag = fom.plusDays(15);
        LocalDate fødselsdato = tjuvefemårsdag.minusYears(25);
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var lavSats = lavSatsBuilder(fom).build();
        var høySats = høySatsBuilder(tjuvefemårsdag).build();

        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), new AktivitetspengerSatsPeriode(new LocalDateInterval(fom, tjuvefemårsdag.minusDays(1)), lavSats)),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), new AktivitetspengerSatsPeriode(new LocalDateInterval(tjuvefemårsdag, p.getTomDato()), høySats))
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), lavSats),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), høySats)
        ));

        // Beregningsgrunnlag svært høyt - overstiger både lav og høy minstesats
        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagSværtHøytBeregningsgrunnlag(fom))
        ));

        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom, tjuvefemårsdag.with(TemporalAdjusters.lastDayOfMonth()));

        return new AktivitetspengerTestScenario(
            DEFAULT_NAVN,
            List.of(new Periode(fom, p.getTomDato())),
            satsperioder,
            beregningsgrunnlag,
            tilkjentYtelsePerioder(lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag), tilkjentPeriode),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            Set.of(new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fra(tjuvefemårsdag, p.getTomDato()))),
            Collections.emptyList(),
            null,
            null);
    }

    private static Beregningsgrunnlag lagHøytBeregningsgrunnlag(LocalDate skjæringstidspunkt) {
        // Beregnet redusert pr år på 200_000 -> overstiger lav minstesats (~177_104) men ikke høy minstesats (~265_657)
        var input = new BeregningInput(new Beløp(400_000), new Beløp(350_000), new Beløp(300_000), skjæringstidspunkt, Year.from(skjæringstidspunkt).minusYears(1));
        return new Beregningsgrunnlag(input, BigDecimal.valueOf(400_000), BigDecimal.valueOf(350_000), BigDecimal.valueOf(400_000), BigDecimal.valueOf(200_000), "test-sporing");
    }

    private static Beregningsgrunnlag lagSværtHøytBeregningsgrunnlag(LocalDate skjæringstidspunkt) {
        // Beregnet redusert pr år på 500_000 -> overstiger også høy minstesats (~265_657)
        var input = new BeregningInput(new Beløp(800_000), new Beløp(700_000), new Beløp(600_000), skjæringstidspunkt, Year.from(skjæringstidspunkt).minusYears(1));
        return new Beregningsgrunnlag(input, BigDecimal.valueOf(800_000), BigDecimal.valueOf(700_000), BigDecimal.valueOf(800_000), BigDecimal.valueOf(500_000), "test-sporing");
    }
}

