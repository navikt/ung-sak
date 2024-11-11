package no.nav.ung.sak.ytelse.ung.uttak;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

class VurderAntallDagerTjeneste {

    public static final long MAKS_ANTALL_DAGER = 260;


    static Optional<UngdomsytelseUttakPerioder> vurderAntallDagerOgLagUttaksperioder(LocalDateTimeline<Boolean> godkjentePerioder) {
        if (godkjentePerioder.isEmpty()) {
            return Optional.empty();
        }
        var perioderMedNokDagerResultat = finnPerioderMedNokDager(godkjentePerioder);

        var perioderEtterOppbrukteDager = godkjentePerioder.disjoint(perioderMedNokDagerResultat.tidslinjeNokDager());

        // Perioder med nok dager får 100% utbetaling enn så lenge
        var uttakPerioder = perioderMedNokDagerResultat.tidslinjeNokDager().getLocalDateIntervals().stream().map(p -> new UngdomsytelseUttakPeriode(BigDecimal.valueOf(100), DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toCollection(ArrayList::new));

        // Perioder etter kvote er brukt opp får 0% utbetaling
        uttakPerioder.addAll(perioderEtterOppbrukteDager.getLocalDateIntervals().stream().map(p -> new UngdomsytelseUttakPeriode(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER, DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toCollection(ArrayList::new)));


        var ungdomsytelseUttakPerioder = new UngdomsytelseUttakPerioder(uttakPerioder);
        ungdomsytelseUttakPerioder.setRegelInput(lagRegelInput(godkjentePerioder));
        ungdomsytelseUttakPerioder.setRegelSporing(lagRegelSporing(perioderMedNokDagerResultat.tidslinjeNokDager(), perioderEtterOppbrukteDager, perioderMedNokDagerResultat.innvilgetDager()));
        return Optional.of(ungdomsytelseUttakPerioder);
    }

    private static String lagRegelSporing(LocalDateTimeline<Boolean> perioderMedNokDager, LocalDateTimeline<Boolean> perioderEtterOppbrukteDager, long antallForbrukteDager) {
        return """
            { "perioderMedNokDager": ":perioderMedNokDager", "perioderEtterOppbrukteDager": ":perioderEtterOppbrukteDager", "antallForbrukteDager": ":antallForbrukteDager" }""".stripLeading()
            .replaceFirst(":perioderMedNokDager", perioderMedNokDager.getLocalDateIntervals().toString())
            .replaceFirst(":perioderEtterOppbrukteDager", perioderEtterOppbrukteDager.getLocalDateIntervals().toString())
            .replaceFirst(":antallForbrukteDager", Long.toString(antallForbrukteDager));
    }

    private static String lagRegelInput(LocalDateTimeline<Boolean> godkjentePerioder) {
        return """
            { "godkjentePerioder": ":godkjentePerioder" }""".stripLeading()
            .replaceFirst(":godkjentePerioder", godkjentePerioder.getLocalDateIntervals().toString());
    }


    private static VurderAntallDagerResultet finnPerioderMedNokDager(LocalDateTimeline<Boolean> godkjentePerioder) {

        var helger = Hjelpetidslinjer.lagTidslinjeMedKunHelger(godkjentePerioder);

        var kunVirkedager = godkjentePerioder.disjoint(helger);

        long antallDager = 0;
        LocalDateTimeline<Boolean> resultatTidslinje = LocalDateTimeline.empty();
        for (LocalDateSegment<Boolean> virkedagSegment : kunVirkedager.toSegments()) {
            var antallDagerISegment = virkedagSegment.getLocalDateInterval().totalDays();
            if (antallDagerISegment > 5) {
                throw new IllegalStateException("Kan ikke ha en sammenhengende periode av virkedager på mer enn 5 dager");
            }
            if (antallDagerISegment + antallDager < MAKS_ANTALL_DAGER) {
                resultatTidslinje = resultatTidslinje.crossJoin(new LocalDateTimeline<>(List.of(virkedagSegment)));
                antallDager += antallDagerISegment;
            } else {
                var delAvPeriode = finnDelAvPeriodeSomInnvilges(virkedagSegment, antallDager);
                resultatTidslinje = resultatTidslinje.crossJoin(delAvPeriode);
                antallDager += delAvPeriode.getLocalDateIntervals().stream().map(LocalDateInterval::totalDays).reduce(Long::sum).orElse(0L);
                break;
            }
        }

        if (antallDager > MAKS_ANTALL_DAGER) {
            throw new IllegalStateException("Skal ikke innvilge mer enn " + MAKS_ANTALL_DAGER + " virkedager");
        }

        if (!resultatTidslinje.isEmpty()) {
            // Legger til helger som ble fjernet for å unngå unødvendig splitt på helg
            return new VurderAntallDagerResultet(leggTilManglendeHelger(resultatTidslinje, helger), antallDager);
        }

        return new VurderAntallDagerResultet(LocalDateTimeline.empty(), antallDager);
    }

    private static LocalDateTimeline<Boolean> finnDelAvPeriodeSomInnvilges(LocalDateSegment<Boolean> virkedagSegment, long antallDager) {
        var dagerSomGjenstår = MAKS_ANTALL_DAGER - antallDager;

        if (dagerSomGjenstår < 1) {
            throw new IllegalStateException("Skal ha minst en dag igjen");
        }

        return new LocalDateTimeline<>(virkedagSegment.getFom(), virkedagSegment.getFom().plusDays(dagerSomGjenstår - 1), Boolean.TRUE);
    }

    private static LocalDateTimeline<Boolean> leggTilManglendeHelger(LocalDateTimeline<Boolean> resultatTidslinje, LocalDateTimeline<Boolean> helgerSomBleFjernet) {
        var medTettetMellomliggendeHelg = tettMellomrom(resultatTidslinje);
        return leggTilbakeHelgerIHverEndeAvSegmenter(helgerSomBleFjernet, medTettetMellomliggendeHelg);
    }

    private static LocalDateTimeline<Boolean> tettMellomrom(LocalDateTimeline<Boolean> resultatTidslinje) {
        var medTettetMellomliggendeHelg = resultatTidslinje.compress(LocalDateInterval::abutsWorkdays, Boolean::equals, StandardCombinators::leftOnly);
        return medTettetMellomliggendeHelg;
    }

    private static LocalDateTimeline<Boolean> leggTilbakeHelgerIHverEndeAvSegmenter(LocalDateTimeline<Boolean> helgerSomBleFjernet, LocalDateTimeline<Boolean> medTettetMellomliggendeHelg) {
        var intervaller = medTettetMellomliggendeHelg.compress().getLocalDateIntervals();
        var tilstøtendeHelger = helgerSomBleFjernet.toSegments().stream().filter(helg -> intervaller.stream().anyMatch(periode -> helg.getLocalDateInterval().abuts(periode))).toList();
        // Legger tilbake helger i endene som ble fjernet
        return medTettetMellomliggendeHelg.crossJoin(new LocalDateTimeline<>(tilstøtendeHelger)).compress();
    }

    record VurderAntallDagerResultet(LocalDateTimeline<Boolean> tidslinjeNokDager, long innvilgetDager) {
    }

}
