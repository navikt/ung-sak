package no.nav.k9.sak.perioder;

import no.nav.k9.sak.behandling.BehandlingReferanse;

import java.util.Map;
import java.util.Set;

public interface SøknadsfristTjeneste {

    Map<Søknad, Set<SøktPeriode>> hentPerioderFor(BehandlingReferanse referanse);
}
