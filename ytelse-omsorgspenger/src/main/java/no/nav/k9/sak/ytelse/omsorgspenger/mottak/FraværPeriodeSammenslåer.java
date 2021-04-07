package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.felles.type.Periode;

import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class FraværPeriodeSammenslåer {
    public static List<FraværPeriode> slåSammen(List<FraværPeriode> fraværperioder) {

        var sortertPeriode = fraværperioder.stream().sorted().collect(Collectors.toList());
        var nyePerioder = new LinkedList<FraværPeriode>();

        for (FraværPeriode søknadPeriode : sortertPeriode) {
            if (nyePerioder.isEmpty()) {
                nyePerioder.add(søknadPeriode);
                continue;
            }
            var sammenslåttPeriode = nyePerioder.getLast();

            var dagerMellan = ChronoUnit.DAYS.between(
                sammenslåttPeriode.getPeriode().getTilOgMed(),
                søknadPeriode.getPeriode().getFraOgMed());

            if (dagerMellan == 1 &&
                søknadPeriode.getDuration().equals(sammenslåttPeriode.getDuration()) &&
                søknadPeriode.getÅrsak().equals(sammenslåttPeriode.getÅrsak()) &&
                søknadPeriode.getAktivitetFravær().equals(sammenslåttPeriode.getAktivitetFravær())) {

                nyePerioder.removeLast();
                nyePerioder.add(new FraværPeriode(
                    new Periode(
                        sammenslåttPeriode.getPeriode().getFraOgMed(),
                        søknadPeriode.getPeriode().getTilOgMed()),
                    søknadPeriode.getDuration(),
                    søknadPeriode.getÅrsak(),
                    søknadPeriode.getAktivitetFravær()
                ));
            } else {
                nyePerioder.add(søknadPeriode);
            }
        }

        return nyePerioder;
    }
}
