package no.nav.ung.sak.formidling;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.behandlingslager.ytelse.sats.GrunnbeløpfaktorTidslinje;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.behandlingslager.ytelse.sats.SatsOgGrunnbeløpfaktor;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.Virkedager;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektInnholdBygger;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.trigger.Trigger;

public class BrevScenarioer {

    public static final String DEFAULT_NAVN = "Ung Testesen";
    private static final BigDecimal G_BELØP_24 = BigDecimal.valueOf(124028);


    public static TestScenarioBuilder lagAvsluttetStandardBehandling(UngTestRepositories repositories) {
        UngTestScenario ungTestscenario = innvilget19år(LocalDate.of(2024, 12, 1));

        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestscenario);

        var behandling = scenarioBuilder.buildOgLagreMedUng(
            repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return scenarioBuilder;
    }

    /**
     * 19 år ungdom med full ungdomsperiode, ingen inntektsgradering og ingen barn
     */
    public static UngTestScenario innvilget19år(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));

        var satser = new LocalDateTimeline<>(p, lavSatsBuilder().build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null);
    }


    /**
     * 27 år ungdom med full ungdomsperiode, ingen inntektsgradering og ingen barn, høy sats
     */
    public static UngTestScenario innvilget27år(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(p,
            høySatsBuilder().build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(27).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null);
    }

    /**
     * 29 år ungdom med ungdomsprogramperiode fram til 29 år, ingen inntektsgradering og ingen barn, høy sats
     */
    public static UngTestScenario innvilget29År(LocalDate fom, LocalDate fødselsdato) {
        var p = new LocalDateInterval(fom, fødselsdato.plusYears(29).with(TemporalAdjusters.lastDayOfMonth()));

        var satser = new LocalDateTimeline<>(p, høySatsBuilder().build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null);
    }

    /**
     * 24 år ungdom med full ungdomsprogramperiode som blir 25 ila perioden. Får både lav og høy sats
     * ingen inntektsgradering og ingen barn TODO: endre denne til å gi lav sats hele perioden
     */
    public static UngTestScenario innvilget24År(LocalDate fom, LocalDate fødselsdato) {
        LocalDate tom25årmnd = fødselsdato.plusYears(25).with(TemporalAdjusters.lastDayOfMonth());
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(p.getFomDato(), tom25årmnd, lavSatsBuilder().build()),
            new LocalDateSegment<>(tom25årmnd.plusDays(1), p.getTomDato(), høySatsBuilder().build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null);
    }


    /**
     * 19 år ungdom med full ungdomsperiode som rapporterer inntekt første måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektPå10k_19år(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        var sats = lavSatsBuilder().build();
        var satser = new LocalDateTimeline<>(p, sats);

        var satserPrMåned = splitPrMåned(satser);
        var førstemåned = satserPrMåned.toSegments().first();
        var rapportertInntektTimeline = new LocalDateTimeline<>(førstemåned.getLocalDateInterval(), BigDecimal.valueOf(10000));
        var tilkjentYtelsePerioder = tilkjentYtelsePerioderMedReduksjon(satserPrMåned, rapportertInntektTimeline);

        var opptjening = OppgittOpptjeningBuilder.ny();
        opptjening.leggTilOppgittArbeidsforhold(OppgittArbeidsforholdBuilder.ny()
            .medInntekt(BigDecimal.valueOf(10000))
            .medPeriode(DatoIntervallEntitet.fra(førstemåned.getLocalDateInterval()))
        );

        var trigger = new Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, DatoIntervallEntitet.fra(satserPrMåned.toSegments().first().getLocalDateInterval()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder,
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(trigger),
            opptjening);
    }

    /**
     * 24 år blir 25 år etter 3 mnd i progrmmet og får overgang til høy sats
     */
    public static UngTestScenario endring25År(LocalDate fødselsdato) {
        var tjuvefemårsdag = fødselsdato.plusYears(25);
        var fom = tjuvefemårsdag.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(3);

        var programPeriode = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(programPeriode.getFomDato(), tjuvefemårsdag.minusDays(1), lavSatsBuilder().build()),
            new LocalDateSegment<>(tjuvefemårsdag, programPeriode.getTomDato(), høySatsBuilder().build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(programPeriode.getFomDato(), programPeriode.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(programPeriode),
            tilkjentYtelsePerioder(satser),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            fødselsdato,
            List.of(programPeriode.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fra(tjuvefemårsdag, programPeriode.getTomDato()))), null);
    }

    private static <T> LocalDateTimeline<T> splitPrMåned(LocalDateTimeline<T> satser) {
        return satser.splitAtRegular(satser.getMinLocalDate().withDayOfMonth(1), satser.getMaxLocalDate(), Period.ofMonths(1));
    }

    private static LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder(LocalDateTimeline<UngdomsytelseSatser> satser) {
        return satser.map(s ->
        {
            BigDecimal multiply = s.getValue().dagsats().multiply(BigDecimal.valueOf(Virkedager.beregnAntallVirkedager(s.getFom(), s.getTom())));
            return List.of(
                new LocalDateSegment<>(s.getLocalDateInterval(), new TilkjentYtelseVerdi(
                    multiply,
                    BigDecimal.ZERO,
                    multiply,
                    s.getValue().dagsats(),
                    100
                ))
            );
        });
    }

    //TODO Endre til å bruke TilkjentYtelseBeregner
    private static LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioderMedReduksjon(LocalDateTimeline<UngdomsytelseSatser> satsperioder, LocalDateTimeline<BigDecimal> rapportertInntektTimeline) {
        return satsperioder.combine(rapportertInntektTimeline,
            (s, lhs, rhs) -> {

                int antallVirkedager = Virkedager.beregnAntallVirkedager(s.getFomDato(), s.getTomDato());

                var uredusertBeløp = lhs.getValue().dagsats().multiply(BigDecimal.valueOf(antallVirkedager));
                var rapportertInntekt = rhs == null ? BigDecimal.ZERO : rhs.getValue();
                var reduksjon = rapportertInntekt.multiply(EndringRapportertInntektInnholdBygger.REDUKSJONS_FAKTOR);
                var redusertBeløp = uredusertBeløp.subtract(reduksjon).max(BigDecimal.ZERO);
                var dagsats = antallVirkedager == 0 ? BigDecimal.ZERO : redusertBeløp.divide(BigDecimal.valueOf(antallVirkedager), 0, RoundingMode.HALF_UP);
                var utbetalingsgrad = redusertBeløp.multiply(BigDecimal.valueOf(100)).divide(uredusertBeløp, 0, RoundingMode.HALF_UP).intValue();

                return new LocalDateSegment<>(s, new TilkjentYtelseVerdi(
                    uredusertBeløp,
                    reduksjon,
                    redusertBeløp,
                    dagsats,
                    utbetalingsgrad));
            }, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }


    @Test
    void testTilkjentYtelseReduksjonScenario() {
        var scenario = endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));
        var førsteMåned = scenario.tilkjentYtelsePerioder().toSegments().first();

        assertThat(førsteMåned.getFom()).isEqualTo(LocalDate.of(2024, 12, 1));
        assertThat(førsteMåned.getTom()).isEqualTo(LocalDate.of(2024, 12, 31));

        //22 virkningsdager i desember 2024 med lav dagsats på 636,04. Rapportert inntekt er 10 000kr
        TilkjentYtelseVerdi t = førsteMåned.getValue();
        assertThat(t.uredusertBeløp()).isEqualByComparingTo("13992.88"); //636,04 * 22
        assertThat(t.reduksjon()).isEqualByComparingTo("6600"); //66% av 10 0000
        assertThat(t.dagsats()).isEqualByComparingTo("336"); //636 - ((6600/22)  )
        assertThat(t.redusertBeløp()).isEqualByComparingTo("7392.88"); // 13992.88 - 6600
        assertThat(t.utbetalingsgrad()).isEqualTo(53); // 7392.88 / 13992.88 * 100

    }

    private static UngdomsytelseUttakPerioder uttaksPerioder(LocalDateInterval p) {
        UngdomsytelseUttakPerioder uttakperioder = new UngdomsytelseUttakPerioder(
            List.of(new UngdomsytelseUttakPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato()))));
        uttakperioder.setRegelInput("regelInputUttak");
        uttakperioder.setRegelSporing("regelSporingUttak");
        return uttakperioder;
    }

    public static UngdomsytelseSatser.Builder lavSatsBuilder() {
        SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor = hentSatstypeOgGrunnbeløp(Sats.LAV);
        return UngdomsytelseSatser.builder()
            .medGrunnbeløp(G_BELØP_24)
            .medGrunnbeløpFaktor(satsOgGrunnbeløpfaktor.grunnbeløpFaktor())
            .medSatstype(satsOgGrunnbeløpfaktor.satstype())
            .medAntallBarn(0)
            .medBarnetilleggDagsats(0);
    }

    public static UngdomsytelseSatser.Builder høySatsBuilder() {
        SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor = hentSatstypeOgGrunnbeløp(Sats.HØY);

        return UngdomsytelseSatser.builder()
            .medGrunnbeløp(G_BELØP_24)
            .medGrunnbeløpFaktor(satsOgGrunnbeløpfaktor.grunnbeløpFaktor())
            .medSatstype(satsOgGrunnbeløpfaktor.satstype())
            .medAntallBarn(0)
            .medBarnetilleggDagsats(0)
            ;
    }

    private static SatsOgGrunnbeløpfaktor hentSatstypeOgGrunnbeløp(Sats sats) {
        return GrunnbeløpfaktorTidslinje.hentGrunnbeløpfaktorTidslinjeFor(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), sats)
        ))).stream().findFirst().orElseThrow().getValue();
    }
}
