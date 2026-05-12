package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.typer.Beløp;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BeregningInput;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

public class AktivitetspengerEndringHøySatsScenarioer {

    /**
     * Overgang fra lav til høy sats (minsteytelse).
     * Bruker fyller 25 år 16 dager etter fom.
     */
    public static AktivitetspengerTestScenario lavTilHøySats(LocalDate fom) {
        LocalDate tjuvefemårsdag = fom.plusDays(15);

        return AktivitetspengerTestScenario.builder(fom)
            .medSatsGrunnlagTidslinjeFyllerTjuefem(tjuvefemårsdag)
            .medStandardBeregningsgrunnlag()
            .medTilkjentPeriode(new LocalDateInterval(fom, tjuvefemårsdag.with(TemporalAdjusters.lastDayOfMonth())))
            .medTrigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, tjuvefemårsdag, fom.plusWeeks(52).minusDays(1))
            .build();
    }

    /**
     * Overgang fra beregningsgrunnlag (lav aldersgruppe) til høy sats (minsteytelse).
     * Beregningsgrunnlag gir høyere dagsats enn minstesats for lav sats,
     * men for høy sats er minstesatsen høyere enn beregningsgrunnlaget.
     */
    public static AktivitetspengerTestScenario beregningsgrunnlagTilHøySats(LocalDate fom) {
        LocalDate tjuvefemårsdag = fom.plusDays(15);

        return AktivitetspengerTestScenario.builder(fom)
            .medSatsGrunnlagTidslinjeFyllerTjuefem(tjuvefemårsdag)
            .medBeregningsgrunnlag(new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, null, lagHøytBeregningsgrunnlag(fom))
            )))
            .medTilkjentPeriode(new LocalDateInterval(fom, tjuvefemårsdag.with(TemporalAdjusters.lastDayOfMonth())))
            .medTrigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, tjuvefemårsdag, fom.plusWeeks(52).minusDays(1))
            .build();
    }

    /**
     * Alle perioder har beregningsgrunnlag som gir høyere dagsats enn minstesatsen, også etter 25 år.
     * I dette tilfellet skal det IKKE sendes brev.
     */
    public static AktivitetspengerTestScenario beregningsgrunnlagHøyereEnnSatsForAllePerioder(LocalDate fom) {
        LocalDate tjuvefemårsdag = fom.plusDays(15);

        return AktivitetspengerTestScenario.builder(fom)
            .medSatsGrunnlagTidslinjeFyllerTjuefem(tjuvefemårsdag)
            .medBeregningsgrunnlag(new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(fom, null, lagSværtHøytBeregningsgrunnlag(fom))
            )))
            .medTilkjentPeriode(new LocalDateInterval(fom, tjuvefemårsdag.with(TemporalAdjusters.lastDayOfMonth())))
            .medTrigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, tjuvefemårsdag, fom.plusWeeks(52).minusDays(1))
            .build();
    }

    private static Beregningsgrunnlag lagHøytBeregningsgrunnlag(LocalDate skjæringstidspunkt) {
        var input = new BeregningInput(new Beløp(400_000), new Beløp(350_000), new Beløp(300_000), skjæringstidspunkt, Year.from(skjæringstidspunkt).minusYears(1));
        return new Beregningsgrunnlag(input, BigDecimal.valueOf(400_000), BigDecimal.valueOf(350_000), BigDecimal.valueOf(400_000), BigDecimal.valueOf(200_000), "test-sporing");
    }

    private static Beregningsgrunnlag lagSværtHøytBeregningsgrunnlag(LocalDate skjæringstidspunkt) {
        var input = new BeregningInput(new Beløp(800_000), new Beløp(700_000), new Beløp(600_000), skjæringstidspunkt, Year.from(skjæringstidspunkt).minusYears(1));
        return new Beregningsgrunnlag(input, BigDecimal.valueOf(800_000), BigDecimal.valueOf(700_000), BigDecimal.valueOf(800_000), BigDecimal.valueOf(500_000), "test-sporing");
    }
}
