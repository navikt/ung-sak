package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestGrunnlag;

public class BrevScenarioer {

    public static final String DEFAULT_NAVN = "Ung Testesen";
    private static final BigDecimal G_BELØP_24 = BigDecimal.valueOf(124028);

    public static TestScenarioBuilder lagAvsluttetStandardBehandling(BehandlingRepositoryProvider repositoryProvider1, UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository1, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository1) {
        UngTestGrunnlag ungTestGrunnlag = innvilget19år(LocalDate.of(2024, 12, 1));

        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestGrunnlag);

        var behandling = scenarioBuilder.buildOgLagreMedUng(repositoryProvider1, ungdomsytelseGrunnlagRepository1, ungdomsprogramPeriodeRepository1);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return scenarioBuilder;
    }

    /**
     * 19 år ungdom med full ungdomsperiode, ingen inntektsgradering og ingen barn
     *
     */
    public static UngTestGrunnlag innvilget19år(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));

        var satser = new LocalDateTimeline<>(p, lavSatsBuilder().build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), TIDENES_ENDE));

        return new UngTestGrunnlag(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42));
    }

    /**
     * 27 år ungdom med full ungdomsperiode, ingen inntektsgradering og ingen barn, høy sats
     *
     */
    public static UngTestGrunnlag innvilget27år(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));

        var satser = new LocalDateTimeline<>(p,
            høySatsBuilder().build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), TIDENES_ENDE));

        return new UngTestGrunnlag(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(27).plusDays(42));
    }

    /**
     * 29 år ungdom med ungdomsprogramperiode fram til 29 år, ingen inntektsgradering og ingen barn, høy sats
     *
     */
    public static UngTestGrunnlag innvilget29År(LocalDate fom, LocalDate fødselsdato) {
        var p = new LocalDateInterval(fom, fødselsdato.plusYears(29).with(TemporalAdjusters.lastDayOfMonth()));

        var satser = new LocalDateTimeline<>(p, høySatsBuilder().build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestGrunnlag(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato);
    }

    /**
     * 25 år ungdom med full ungdomsprogramperiode som blir 26 ila perioden. Får både lav og høy sats
     * ingen inntektsgradering og ingen barn, høy sats
     *
     */
    public static UngTestGrunnlag innvilget26År(LocalDate fom, LocalDate fødselsdato) {
        LocalDate tom26årmnd = fødselsdato.plusYears(26).with(TemporalAdjusters.lastDayOfMonth());
        var p = new LocalDateInterval(fom, fom.plusYears(1));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(p.getFomDato(), tom26årmnd, lavSatsBuilder().build()),
            new LocalDateSegment<>(tom26årmnd.plusDays(1), p.getTomDato(), høySatsBuilder().build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestGrunnlag(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato);
    }

    private static UngdomsytelseUttakPerioder uttaksPerioder(LocalDateInterval p) {
        UngdomsytelseUttakPerioder uttakperioder = new UngdomsytelseUttakPerioder(
            List.of(new UngdomsytelseUttakPeriode(
                BigDecimal.valueOf(100), DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato()))
            ));
        uttakperioder.setRegelInput("regelInputUttak");
        uttakperioder.setRegelSporing("regelSporingUttak");
        return uttakperioder;
    }

    public static UngdomsytelseSatser.Builder lavSatsBuilder() {
        return UngdomsytelseSatser.builder()
            .medGrunnbeløp(G_BELØP_24)
            .medGrunnbeløpFaktor(Sats.LAV.getGrunnbeløpFaktor())
            .medSatstype(Sats.LAV.getSatsType())
            .medAntallBarn(0)
            .medBarnetilleggDagsats(0);
    }

    public static UngdomsytelseSatser.Builder høySatsBuilder() {
        return UngdomsytelseSatser.builder()
            .medGrunnbeløp(G_BELØP_24)
            .medGrunnbeløpFaktor(Sats.HØY.getGrunnbeløpFaktor())
            .medSatstype(Sats.HØY.getSatsType())
            .medAntallBarn(0)
            .medBarnetilleggDagsats(0)
            ;
    }
}
