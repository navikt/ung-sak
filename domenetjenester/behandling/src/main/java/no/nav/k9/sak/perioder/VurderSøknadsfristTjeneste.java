package no.nav.k9.sak.perioder;

import java.util.Map;
import java.util.Set;

public interface VurderSøknadsfristTjeneste {

    Map<Søknad, Set<VurdertSøktPeriode>> vurderSøknadsfrist(Map<Søknad, Set<SøktPeriode>> søknaderMedPerioder);
}
