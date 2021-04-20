package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.felles.type.Periode;

public class FraværPeriodeSammenslåer {
    public static List<FraværPeriode> slåSammen(List<FraværPeriode> søknadsperioder) {
        var sorterteSøknadsperioder = søknadsperioder.stream().sorted().collect(Collectors.toList());
        var sammenslåttePerioder = new LinkedList<FraværPeriode>();

        for (FraværPeriode søknadPeriode : sorterteSøknadsperioder) {
            if (sammenslåttePerioder.isEmpty()) {
                sammenslåttePerioder.add(søknadPeriode);
                continue;
            }
            var sammenslåttPeriode = sammenslåttePerioder.getLast();

            boolean periodenLiggerInntilForrigePeriode = ChronoUnit.DAYS.between(sammenslåttPeriode.getPeriode().getTilOgMed(), søknadPeriode.getPeriode().getFraOgMed()) == 1;
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
}
