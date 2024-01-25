package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplanForBehandlinger;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;

public class OMPUttakEndringsutleder {


    private OMPUttakEndringsutleder() {
        // SKjul konstruktør
    }

    static boolean harRelevantEndringFraForrige(Uttaksperiode periode, Optional<FullUttaksplanForBehandlinger> fullUttaksplanForrigeBehandling) {

        var overlappendePerioder = fullUttaksplanForrigeBehandling.map(p -> p
            .getAktiviteter()
            .stream()
            .map(Aktivitet::getUttaksperioder)
            .flatMap(Collection::stream)
            .filter(it -> it.getPeriode().overlapper(periode.getPeriode()))
            .collect(Collectors.toSet())).orElse(Collections.emptySet());

        if (overlappendePerioder.size() != 1) {
            return true;
        }

        var forrigePeriode = overlappendePerioder.iterator().next();


        if (!Objects.equals(periode.getPeriode(), forrigePeriode.getPeriode())) {
            return true;
        }

        if (!Objects.equals(periode.getUtbetalingsgrad(), forrigePeriode.getUtbetalingsgrad())) {
            return true;
        }


        if (!Objects.equals(periode.getRefusjonTilArbeidsgiver(), forrigePeriode.getRefusjonTilArbeidsgiver())) {
            return true;
        }


        if (!Objects.equals(periode.getUtfall(), forrigePeriode.getUtfall())) {
            return true;
        }

        if (!Objects.equals(periode.getAvvikImSøknad(), forrigePeriode.getAvvikImSøknad())) {
            return true;
        }

        return false;
    }


}
