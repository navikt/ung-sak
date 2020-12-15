package no.nav.k9.sak.perioder;

import no.nav.k9.sak.behandling.BehandlingReferanse;

import java.util.Map;
import java.util.Set;

public interface VurderSøknadsfristTjeneste {

    Map<Søknad, Set<SøktPeriode>> hentPerioder(BehandlingReferanse referanse);

    Map<Søknad, Set<VurdertSøktPeriode>> vurderSøknadsfrist(Map<Søknad, Set<SøktPeriode>> søknaderMedPerioder);
}
