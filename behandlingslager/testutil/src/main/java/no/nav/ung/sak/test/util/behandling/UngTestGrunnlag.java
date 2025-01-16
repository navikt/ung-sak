package no.nav.ung.sak.test.util.behandling;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Hjelpeobjekt for å populere databasen med diverse ung data. Brukes av TestScenarioBuilder
 *
 * @param alder
 * @param programPerioder - perioder ungdommen er i programmet, kan være stykkevis
 * @param satser - timeline med satser og når de gjelder. Bruk gjerne statiske hjelpebuildere fra denne klassen
 * @param uttakPerioder - perioder med uttak, kan evt legge på gradering her
 * @param aldersvilkår - timeline med aldersvilkår oppfylt og ikke oppfylt
 * @param ungdomsprogramvilkår - timeline med ungdomsprogramvilkår oppfylt og ikke oppfylt
 */
public record UngTestGrunnlag(
    int alder,
    String navn,
    List<UngdomsprogramPeriode> programPerioder,
    LocalDateTimeline<UngdomsytelseSatser> satser,
    UngdomsytelseUttakPerioder uttakPerioder,
    LocalDateTimeline<Utfall> aldersvilkår,
    LocalDateTimeline<Utfall> ungdomsprogramvilkår
) {

    private static final BigDecimal G_BELØP_24 = BigDecimal.valueOf(124028);
    public static final String DEFAULT_NAVN = "Ung Testesen";


    /**
     * 19 år ungdom med full ungdomsperiode, ingen inntektsgradering og ingen barn
     *
     */
    public static UngTestGrunnlag standardInnvilget(LocalDate fom) {
        var p = new LocalDateInterval(fom, LocalDate.now().plusYears(1));

        var satser = new LocalDateTimeline<>(p,
            lavSatsBuilder()
                .medAntallBarn(0)
                .medBarnetilleggDagsats(0)
                .build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), TIDENES_ENDE));

        return new UngTestGrunnlag(
            19,
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT)
        );
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
            .medSatstype(Sats.LAV.getSatsType());
    }

    public static UngdomsytelseSatser.Builder høySatsBuilder() {
        return UngdomsytelseSatser.builder()
            .medGrunnbeløp(G_BELØP_24)
            .medGrunnbeløpFaktor(Sats.HØY.getGrunnbeløpFaktor())
            .medSatstype(Sats.HØY.getSatsType());
    }

}


