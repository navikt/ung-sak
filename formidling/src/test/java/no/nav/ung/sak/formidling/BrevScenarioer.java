package no.nav.ung.sak.formidling;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.GrunnbeløpfaktorTidslinje;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.behandlingslager.ytelse.sats.SatsOgGrunnbeløpfaktor;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.Virkedager;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestscenario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

public class BrevScenarioer {

    public static final String DEFAULT_NAVN = "Ung Testesen";
    private static final BigDecimal G_BELØP_24 = BigDecimal.valueOf(124028);

    public static TestScenarioBuilder lagAvsluttetStandardBehandling(BehandlingRepositoryProvider repositoryProvider1, UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository1, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository1, TilkjentYtelseRepository tilkjentYtelseRepository) {
        UngTestscenario ungTestscenario = innvilget19år(LocalDate.of(2024, 12, 1));

        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestscenario);

        var behandling = scenarioBuilder.buildOgLagreMedUng(repositoryProvider1, ungdomsytelseGrunnlagRepository1, ungdomsprogramPeriodeRepository1, tilkjentYtelseRepository);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return scenarioBuilder;
    }

    /**
     * 19 år ungdom med full ungdomsperiode, ingen inntektsgradering og ingen barn
     */
    public static UngTestscenario innvilget19år(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));

        var satser = new LocalDateTimeline<>(p, lavSatsBuilder().build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), TIDENES_ENDE));

        return new UngTestscenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42));
    }

    private static LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder(LocalDateTimeline<UngdomsytelseSatser> satser) {
        return satser.map(s ->
            List.of(
                new LocalDateSegment<>(s.getLocalDateInterval(), new TilkjentYtelseVerdi(
                    s.getValue().dagsats().multiply(BigDecimal.valueOf(Virkedager.beregnAntallVirkedager(s.getFom(), s.getTom()))),
                    BigDecimal.ZERO,
                    s.getValue().dagsats().multiply(BigDecimal.valueOf(Virkedager.beregnAntallVirkedager(s.getFom(), s.getTom()))),
                    s.getValue().dagsats(),
                    100
                ))
            ));
    }

    /**
     * 27 år ungdom med full ungdomsperiode, ingen inntektsgradering og ingen barn, høy sats
     */
    public static UngTestscenario innvilget27år(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));

        var satser = new LocalDateTimeline<>(p,
            høySatsBuilder().build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), TIDENES_ENDE));

        return new UngTestscenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(27).plusDays(42));
    }

    /**
     * 29 år ungdom med ungdomsprogramperiode fram til 29 år, ingen inntektsgradering og ingen barn, høy sats
     */
    public static UngTestscenario innvilget29År(LocalDate fom, LocalDate fødselsdato) {
        var p = new LocalDateInterval(fom, fødselsdato.plusYears(29).with(TemporalAdjusters.lastDayOfMonth()));

        var satser = new LocalDateTimeline<>(p, høySatsBuilder().build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestscenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato);
    }

    /**
     * 25 år ungdom med full ungdomsprogramperiode som blir 26 ila perioden. Får både lav og høy sats
     * ingen inntektsgradering og ingen barn, høy sats
     */
    public static UngTestscenario innvilget26År(LocalDate fom, LocalDate fødselsdato) {
        LocalDate tom26årmnd = fødselsdato.plusYears(26).with(TemporalAdjusters.lastDayOfMonth());
        var p = new LocalDateInterval(fom, fom.plusYears(1));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(p.getFomDato(), tom26årmnd, lavSatsBuilder().build()),
            new LocalDateSegment<>(tom26årmnd.plusDays(1), p.getTomDato(), høySatsBuilder().build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestscenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato);
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
