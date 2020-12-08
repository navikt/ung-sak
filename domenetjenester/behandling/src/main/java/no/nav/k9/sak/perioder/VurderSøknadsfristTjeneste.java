package no.nav.k9.sak.perioder;

import no.nav.k9.sak.behandling.BehandlingReferanse;

import java.util.Map;
import java.util.Set;

public interface VurderSøknadsfristTjeneste {

    Map<Søknad, Set<VurdertSøktPeriode>> vurderSøknadsfrist(BehandlingReferanse behandlingReferanse, Map<Søknad, Set<SøktPeriode>> søknaderMedPerioder);
}
