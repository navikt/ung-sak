package no.nav.ung.sak.ungdomsprogram.forbruktedager;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.sak.domene.typer.tid.Hjelpetidslinjer;

public class FinnForbrukteDager {

    public static final long MAKS_ANTALL_DAGER = 260;

    public static VurderAntallDagerResultat finnForbrukteDager(LocalDateTimeline<Boolean> ungdomsprogramperiode) {

        var helger = Hjelpetidslinjer.lagTidslinjeMedKunHelger(ungdomsprogramperiode);

        var kunVirkedager = ungdomsprogramperiode.disjoint(helger);

        int antallDager = 0;
        LocalDateTimeline<Boolean> resultatTidslinje = LocalDateTimeline.empty();
        for (LocalDateSegment<Boolean> virkedagSegment : kunVirkedager.toSegments()) {
            var antallDagerISegment = virkedagSegment.getLocalDateInterval().totalDays();
            if (antallDagerISegment > 5) {
                throw new IllegalStateException("Kan ikke ha en sammenhengende periode av virkedager på mer enn 5 dager");
            }
            if (antallDagerISegment + antallDager < MAKS_ANTALL_DAGER) {
                resultatTidslinje = resultatTidslinje.crossJoin(new LocalDateTimeline<>(List.of(virkedagSegment)));
                antallDager += (int) antallDagerISegment;
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
            return new VurderAntallDagerResultat(leggTilManglendeHelger(resultatTidslinje, helger), antallDager);
        }

        return new VurderAntallDagerResultat(LocalDateTimeline.empty(), antallDager);
    }

    private static LocalDateTimeline<Boolean> finnDelAvPeriodeSomInnvilges(LocalDateSegment<Boolean> virkedagSegment, int antallDager) {
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

}
