package no.nav.ung.sak.ytelse.ung.uttak;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;
import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

class VurderAntallDagerTjeneste {


    static Optional<UngdomsytelseUttakPerioder> vurderAntallDagerOgLagUttaksperioder(LocalDateTimeline<Boolean> godkjentePerioder,
                                                                                     LocalDateTimeline<Boolean> ungdomsprogramtidslinje,
                                                                                     Optional<LocalDate> søkersDødsdato) {
        if (godkjentePerioder.isEmpty()) {
            return Optional.empty();
        }

        var levendeBrukerTidslinje = søkersDødsdato.map(d -> new LocalDateTimeline<>(TIDENES_BEGYNNELSE, d, true)).orElse(new LocalDateTimeline<>(TIDENES_BEGYNNELSE, TIDENES_ENDE, true));

        var perioderMedNokDagerResultat = FinnForbrukteDager.finnForbrukteDager(ungdomsprogramtidslinje);

        var perioderEtterOppbrukteDager = godkjentePerioder.disjoint(perioderMedNokDagerResultat.tidslinjeNokDager());

        // Perioder med nok dager får 100% utbetaling enn så lenge
        var tidslinjeNokDagerOgUtbetaling = finnTidslinjeNokDagerOgUtbetaling(godkjentePerioder, perioderMedNokDagerResultat, levendeBrukerTidslinje);
        var uttakPerioder = mapTilUttakPerioderMedNokDagerOgUtbetaling(tidslinjeNokDagerOgUtbetaling);

        // Perioder med nok dager etter søkers dødsfall får 0% i utbetaling
        var avslåttEtterSøkersDødTidslinje = finnTidslinjeAvslåttGrunnetSøkersDød(godkjentePerioder, perioderMedNokDagerResultat, levendeBrukerTidslinje);
        uttakPerioder.addAll(mapTilUttakPerioderAvslåttGrunnetSøkersDødfall(avslåttEtterSøkersDødTidslinje));

        // Perioder etter kvote er brukt opp får 0% utbetaling
        uttakPerioder.addAll(perioderEtterOppbrukteDager
            .getLocalDateIntervals()
            .stream()
            .map(p -> new UngdomsytelseUttakPeriode(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER, DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toCollection(ArrayList::new)));


        var ungdomsytelseUttakPerioder = new UngdomsytelseUttakPerioder(uttakPerioder);
        ungdomsytelseUttakPerioder.setRegelInput(lagRegelInput(godkjentePerioder, ungdomsprogramtidslinje, søkersDødsdato));
        ungdomsytelseUttakPerioder.setRegelSporing(lagRegelSporing(
            perioderMedNokDagerResultat.tidslinjeNokDager(),
            perioderEtterOppbrukteDager,
            avslåttEtterSøkersDødTidslinje,
            perioderMedNokDagerResultat.forbrukteDager()));
        return Optional.of(ungdomsytelseUttakPerioder);
    }

    private static List<UngdomsytelseUttakPeriode> mapTilUttakPerioderAvslåttGrunnetSøkersDødfall(LocalDateTimeline<Boolean> avslåttEtterSøkersDødTidslinje) {
        return avslåttEtterSøkersDødTidslinje
            .getLocalDateIntervals()
            .stream()
            .map(p -> new UngdomsytelseUttakPeriode(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL, DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static LocalDateTimeline<Boolean> finnTidslinjeAvslåttGrunnetSøkersDød(LocalDateTimeline<Boolean> godkjentePerioder, VurderAntallDagerResultat perioderMedNokDagerResultat, LocalDateTimeline<Boolean> levendeBrukerTidslinje) {
        return perioderMedNokDagerResultat.tidslinjeNokDager()
            .intersection(godkjentePerioder)
            .disjoint(levendeBrukerTidslinje);
    }

    private static ArrayList<UngdomsytelseUttakPeriode> mapTilUttakPerioderMedNokDagerOgUtbetaling(LocalDateTimeline<Boolean> tidslinjeNokDagerOgUtbetaling) {
        return tidslinjeNokDagerOgUtbetaling
            .getLocalDateIntervals()
            .stream()
            .map(p -> new UngdomsytelseUttakPeriode(BigDecimal.valueOf(100), DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static LocalDateTimeline<Boolean> finnTidslinjeNokDagerOgUtbetaling(LocalDateTimeline<Boolean> godkjentePerioder,
                                                                                VurderAntallDagerResultat perioderMedNokDagerResultat,
                                                                                LocalDateTimeline<Boolean> levendeBrukerTidslinje) {
        return perioderMedNokDagerResultat.tidslinjeNokDager()
            .intersection(godkjentePerioder)
            .intersection(levendeBrukerTidslinje);
    }

    private static String lagRegelSporing(LocalDateTimeline<Boolean> perioderMedNokDager,
                                          LocalDateTimeline<Boolean> perioderEtterOppbrukteDager,
                                          LocalDateTimeline<Boolean> perioderAvslåttEtterSøkersDød,
                                          long antallForbrukteDager) {
        return """
            { "perioderMedNokDager": ":perioderMedNokDager", "perioderEtterOppbrukteDager": ":perioderEtterOppbrukteDager", "antallForbrukteDager": ":antallForbrukteDager",
            "perioderAvslåttEtterSøkersDød": :perioderAvslåttEtterSøkersDød }""".stripLeading()
            .replaceFirst(":perioderMedNokDager", perioderMedNokDager.getLocalDateIntervals().toString())
            .replaceFirst(":perioderEtterOppbrukteDager", perioderEtterOppbrukteDager.getLocalDateIntervals().toString())
            .replaceFirst(":antallForbrukteDager", Long.toString(antallForbrukteDager))
            .replaceFirst(":perioderAvslåttEtterSøkersDød", perioderAvslåttEtterSøkersDød.getLocalDateIntervals().toString());
    }

    private static String lagRegelInput(LocalDateTimeline<Boolean> godkjentePerioder, LocalDateTimeline<Boolean> ungdomsprogramtidslinje, Optional<LocalDate> søkersDødsdato) {
        return """
            {
                "godkjentePerioder": ":godkjentePerioder",
                "ungdomsprogramtidslinje": ":ungdomsprogramtidslinje",
                "søkersDødsdato": :søkersDødsdato
            }
            """.stripLeading()
            .replaceFirst(":godkjentePerioder", godkjentePerioder.getLocalDateIntervals().toString())
            .replaceFirst(":ungdomsprogramtidslinje", ungdomsprogramtidslinje.getLocalDateIntervals().toString())
            .replaceFirst(":søkersDødsdato", søkersDødsdato.map(Objects::toString).map(it -> "\"" + it + "\"").orElse("null"));
    }

}
