package no.nav.ung.sak.formidling.scenarioer;


import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.behandlingslager.ytelse.sats.*;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.ytelse.BeregnetSats;
import no.nav.ung.sak.ytelse.TilkjentYtelseBeregner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

public class BrevScenarioerUtils {

    public static final String DEFAULT_NAVN = "Ung Testesen";

    static PersonInformasjon lagBarn(LocalDate barnFødselsdato) {
        return PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT).medPersonas().barn(AktørId.dummy(), barnFødselsdato).build();
    }

    static PersonInformasjon lagBarnMedDødsdato(LocalDate barnFødselsdato, LocalDate barnDødsdato) {
        return PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT).medPersonas().barn(AktørId.dummy(), barnFødselsdato).dødsdato(barnDødsdato).build();
    }


    static <T> LocalDateTimeline<T> splitPrMåned(LocalDateTimeline<T> satser) {
        return satser.splitAtRegular(satser.getMinLocalDate().withDayOfMonth(1), satser.getMaxLocalDate(), Period.ofMonths(1));
    }

    static LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder(LocalDateTimeline<UngdomsytelseSatser> satser, LocalDateInterval tilkjentPeriode) {
        return tilkjentYtelsePerioderMedReduksjon(satser, tilkjentPeriode, LocalDateTimeline.empty());
    }

    static LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioderMedReduksjon(LocalDateTimeline<UngdomsytelseSatser> satsperioder, LocalDateInterval tilkjentPeriode, LocalDateTimeline<BigDecimal> rapportertInntektTimeline) {
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


    static UngdomsytelseUttakPerioder uttaksPerioder(LocalDateInterval p) {
        UngdomsytelseUttakPerioder uttakperioder = new UngdomsytelseUttakPerioder(
            List.of(new UngdomsytelseUttakPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato()))));
        uttakperioder.setRegelInput("regelInputUttak");
        uttakperioder.setRegelSporing("regelSporingUttak");
        return uttakperioder;
    }

    public static UngdomsytelseSatser.Builder lavSatsBuilder(LocalDate fom) {
        return lavSatsMedBarnBuilder(fom, 0);
    }

    public static UngdomsytelseSatser.Builder lavSatsMedBarnBuilder(LocalDate fom, int antallBarn) {
        SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor = hentSatstypeOgGrunnbeløp(Sats.LAV);
        var barneTillegg = BarnetilleggSatsTidslinje.BARNETILLEGG_DAGSATS.getSegment(new LocalDateInterval(fom, fom)).getValue();
        BigDecimal g = hentGrunnbeløpFor(fom);
        return UngdomsytelseSatser.builder()
            .medGrunnbeløp(g)
            .medGrunnbeløpFaktor(satsOgGrunnbeløpfaktor.grunnbeløpFaktor())
            .medSatstype(satsOgGrunnbeløpfaktor.satstype())
            .medAntallBarn(antallBarn)
            .medBarnetilleggDagsats(beregnDagsatsInklBarnetillegg(antallBarn, barneTillegg).intValue() );
    }

    public static UngdomsytelseSatser.Builder høySatsBuilder(LocalDate fom) {
        return høySatsBuilderMedBarn(fom, 0);
    }

    public static UngdomsytelseSatser.Builder høySatsBuilderMedBarn(LocalDate fom, int antallBarn) {
        SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor = hentSatstypeOgGrunnbeløp(Sats.HØY);
        var barneTillegg = BarnetilleggSatsTidslinje.BARNETILLEGG_DAGSATS.getSegment(new LocalDateInterval(fom, fom)).getValue();
        var g = hentGrunnbeløpFor(fom);
        return UngdomsytelseSatser.builder()
            .medGrunnbeløp(g)
            .medGrunnbeløpFaktor(satsOgGrunnbeløpfaktor.grunnbeløpFaktor())
            .medSatstype(satsOgGrunnbeløpfaktor.satstype())
            .medAntallBarn(antallBarn)
            .medBarnetilleggDagsats(beregnDagsatsInklBarnetillegg(antallBarn, barneTillegg).intValue() );
    }

    private static BigDecimal hentGrunnbeløpFor(LocalDate fom) {
        return GrunnbeløpTidslinje.hentTidslinje().getSegment(new LocalDateInterval(fom, fom)).getValue().verdi();
    }

    private static BigDecimal beregnDagsatsInklBarnetillegg(int antallBarn, BigDecimal barneTillegg) {
        return barneTillegg.multiply(BigDecimal.valueOf(antallBarn));
    }

    private static SatsOgGrunnbeløpfaktor hentSatstypeOgGrunnbeløp(Sats sats) {
        return GrunnbeløpfaktorTidslinje.hentGrunnbeløpfaktorTidslinjeFor(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), sats)
        ))).stream().findFirst().orElseThrow().getValue();
    }

}
