package no.nav.ung.sak.formidling;


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
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.ytelse.BeregnetSats;
import no.nav.ung.sak.ytelse.TilkjentYtelseBeregner;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class BrevScenarioer {

    public static final String DEFAULT_NAVN = "Ung Testesen";
    private static final BigDecimal G_BELØP_24 = BigDecimal.valueOf(124028);


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

        var satser = new LocalDateTimeline<>(p, lavSatsBuilder().build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
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
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
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
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null);
    }

    /**
     * Søker bakover i tid med startdato lik første i måneden fylte 25 år.
     * Søker måneden etter fylte 25 år.
     * Så er 24 år ved startdato.
     * Får både lav og høy sats i førstegangsbehandlingen
     * ingen inntektsgradering og ingen barn
     */
    public static UngTestScenario innvilget25årBle25årførsteMåned(LocalDate fødselsdato) {
        LocalDate tjuvefemårsdag = fødselsdato.plusYears(25);
        LocalDate tom25årmnd = tjuvefemårsdag.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate fom25årmnd = tjuvefemårsdag.with(TemporalAdjusters.firstDayOfMonth());
        var p = new LocalDateInterval(fom25årmnd, fom25årmnd.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom25årmnd, tjuvefemårsdag.minusDays(1), lavSatsBuilder().build()),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), høySatsBuilder().build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom25årmnd, tom25årmnd);
        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser, tilkjentPeriode),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null);
    }


    /**
     * 19 år ungdom med full ungdomsperiode som rapporterer inntekt andre måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektPå10k_19år(LocalDate fom) {
        return endringMedInntekt_19år(fom,
            new LocalDateInterval(fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1)
                    .with(TemporalAdjusters.lastDayOfMonth())), 10000);
    }

    /**
     * 19 år ungdom med full ungdomsperiode som rapporterer inntekt andre og tredje måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektPå10k_flere_mnd_19år(LocalDate fom) {
        LocalDateInterval rapportertInntektPeriode = new LocalDateInterval(
            fom.withDayOfMonth(1).plusMonths(1),
            fom.withDayOfMonth(1).plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()));

        return endringMedInntekt_19år(fom, rapportertInntektPeriode, 10000);
    }

    /**
     * 19 år ungdom med full ungdomsperiode uten inntekt og rapporterer ingen inntekt
     */
    public static UngTestScenario endring0KrInntekt_19år(LocalDate fom) {
        return endringMedInntekt_19år(fom,
            new LocalDateInterval(fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1)
                    .with(TemporalAdjusters.lastDayOfMonth())), null);
    }

    @NotNull
    private static UngTestScenario endringMedInntekt_19år(LocalDate fom, LocalDateInterval rapportertInntektPeriode, Integer rapportertInntektPrMåned) {
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        var sats = lavSatsBuilder().build();
        var satser = new LocalDateTimeline<>(p, sats);

        var satserPrMåned = splitPrMåned(satser);
        var rapportertInntektTimeline = splitPrMåned(new LocalDateTimeline<>(rapportertInntektPeriode, rapportertInntektPrMåned != null ? BigDecimal.valueOf(rapportertInntektPrMåned) : BigDecimal.ZERO));
        var tilkjentYtelsePerioder = tilkjentYtelsePerioderMedReduksjon(satserPrMåned, rapportertInntektPeriode, rapportertInntektTimeline);


        var opptjening = OppgittOpptjeningBuilder.ny();

        rapportertInntektTimeline.forEach(it ->
            opptjening.leggTilOppgittArbeidsforhold(OppgittArbeidsforholdBuilder.ny()
                .medInntekt(it.getValue())
                .medPeriode(DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            ));

        var triggere = HashSet.<Trigger>newHashSet(2);
        triggere.add(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fra(rapportertInntektPeriode)));
        if (rapportertInntektPrMåned != null) {
            triggere.add(new Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, DatoIntervallEntitet.fra(rapportertInntektPeriode)));
        }

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
            triggere,
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
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            fødselsdato,
            List.of(programPeriode.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fra(tjuvefemårsdag, programPeriode.getTomDato()))), null);
    }

    private static <T> LocalDateTimeline<T> splitPrMåned(LocalDateTimeline<T> satser) {
        return satser.splitAtRegular(satser.getMinLocalDate().withDayOfMonth(1), satser.getMaxLocalDate(), Period.ofMonths(1));
    }

    private static LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder(LocalDateTimeline<UngdomsytelseSatser> satser, LocalDateInterval tilkjentPeriode) {
        return tilkjentYtelsePerioderMedReduksjon(satser, tilkjentPeriode, LocalDateTimeline.empty());

    }

    private static LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioderMedReduksjon(LocalDateTimeline<UngdomsytelseSatser> satsperioder, LocalDateInterval tilkjentPeriode, LocalDateTimeline<BigDecimal> rapportertInntektTimeline) {
        LocalDateTimeline<Boolean> ytelseTidslinje = splitPrMåned(new LocalDateTimeline<>(tilkjentPeriode, true));
        LocalDateTimeline<BeregnetSats> beregnetSats = TilkjentYtelseBeregner.mapSatserTilTotalbeløpForPerioder(satsperioder, ytelseTidslinje);
        return beregnetSats.intersection(ytelseTidslinje).combine(rapportertInntektTimeline,
            (s, lhs, rhs) -> {
                var rapportertInntekt = rhs == null ? BigDecimal.ZERO : rhs.getValue();
                return new LocalDateSegment<>(s, TilkjentYtelseBeregner.beregn(s, lhs.getValue(), rapportertInntekt).verdi());
            },
            LocalDateTimeline.JoinStyle.LEFT_JOIN
        );
    }


    @Test
    void testTilkjentYtelseReduksjonScenario() {
        var scenario = endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));
        var andreMåned = scenario.tilkjentYtelsePerioder().getSegment(new LocalDateInterval(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)));

        assertThat(andreMåned.getFom()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(andreMåned.getTom()).isEqualTo(LocalDate.of(2025, 1, 31));

        //23 virkningsdager i januar 2025 med lav dagsats på 636,04. Rapportert inntekt er 10 000kr
        TilkjentYtelseVerdi t = andreMåned.getValue();
        assertThat(t.uredusertBeløp()).isEqualByComparingTo("14628.92"); //636,04 * 23
        assertThat(t.reduksjon()).isEqualByComparingTo("6600"); //66% av 10 0000
        assertThat(t.dagsats()).isEqualByComparingTo("349"); //636 - ((6600/22)  )
        assertThat(t.redusertBeløp()).isEqualByComparingTo("8028.92"); // 14628.92 - 6600
        assertThat(t.utbetalingsgrad()).isEqualTo(55); // 8028.92 / 14628.92 * 100

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
