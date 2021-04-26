package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.felles.type.Periode;

public class FraværPeriodeSammenslåer {
    public static List<FraværPeriode> fjernHelgOgSlåSammen(List<FraværPeriode> søknadsperioder) {
        List<FraværPeriode> fraværsPerioderUtenHelgdager = fjernHelg(søknadsperioder);
        var sorterteSøknadsperioderUtenHelgdager = fraværsPerioderUtenHelgdager.stream().sorted().collect(Collectors.toList());
        return slåSammenIntilliggendePerioder(sorterteSøknadsperioderUtenHelgdager);
    }

    private static List<FraværPeriode> fjernHelg(List<FraværPeriode> søknadsperioder) {
        var fraværsPerioderUtenHelgdager = new ArrayList<FraværPeriode>();
        for (FraværPeriode fp : søknadsperioder) {
            var perioderMinusHelgdager = fjernHelgdager(fp.getPeriode());
            for (Periode periode : perioderMinusHelgdager) {
                fraværsPerioderUtenHelgdager.add(new FraværPeriode(periode, fp.getDuration(), fp.getÅrsak(), fp.getAktivitetFravær()));
            }
        }
        return fraværsPerioderUtenHelgdager;
    }

    private static List<FraværPeriode> slåSammenIntilliggendePerioder(List<FraværPeriode> sorterteSøknadsperioderUtenHelgdager) {
        var sammenslåttePerioder = new LinkedList<FraværPeriode>();

        for (FraværPeriode søknadPeriode : sorterteSøknadsperioderUtenHelgdager) {
            if (sammenslåttePerioder.isEmpty()) {
                sammenslåttePerioder.add(søknadPeriode);
                continue;
            }
            var sammenslåttPeriode = sammenslåttePerioder.getLast();

            boolean periodenLiggerInntilForrigePeriode =
                ChronoUnit.DAYS.between(sammenslåttPeriode.getPeriode().getTilOgMed(), søknadPeriode.getPeriode().getFraOgMed()) == 1;

            if (periodenLiggerInntilForrigePeriode &&
                Optional.ofNullable(søknadPeriode.getDuration()).equals(Optional.ofNullable(sammenslåttPeriode.getDuration())) &&
                søknadPeriode.getÅrsak().equals(sammenslåttPeriode.getÅrsak()) &&
                søknadPeriode.getAktivitetFravær().equals(sammenslåttPeriode.getAktivitetFravær())) {

                sammenslåttePerioder.removeLast();
                sammenslåttePerioder.add(new FraværPeriode(
                    new Periode(
                        sammenslåttPeriode.getPeriode().getFraOgMed(),
                        søknadPeriode.getPeriode().getTilOgMed()),
                    søknadPeriode.getDuration(),
                    søknadPeriode.getÅrsak(),
                    søknadPeriode.getAktivitetFravær()
                ));
            } else {
                sammenslåttePerioder.add(søknadPeriode);
            }
        }

        return sammenslåttePerioder;
    }

    static List<Periode> fjernHelgdager(Periode periode) {
        var resultat = new ArrayList<Periode>();
        var fom = periode.getFraOgMed();
        LocalDate startPåNyPeriode = null;
        while (fom.isBefore(periode.getTilOgMed().plusDays(1))) {
            if (startPåNyPeriode == null && !(fom.getDayOfWeek().equals(SATURDAY) || fom.getDayOfWeek().equals(SUNDAY))) {
                startPåNyPeriode = fom;
            }
            if (startPåNyPeriode != null && fom.getDayOfWeek().equals(SATURDAY)) {
                resultat.add(new Periode(startPåNyPeriode, fom.minusDays(1L)));
                startPåNyPeriode = null;
            } else if (startPåNyPeriode != null && fom.isEqual(periode.getTilOgMed())) {
                resultat.add(new Periode(startPåNyPeriode, periode.getTilOgMed()));
            }
            fom = fom.plusDays(1L);
        }
        return resultat;
    }
}
