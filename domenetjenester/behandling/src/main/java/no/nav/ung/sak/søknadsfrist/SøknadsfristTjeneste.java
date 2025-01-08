package no.nav.ung.sak.søknadsfrist;

import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;

public interface SøknadsfristTjeneste {

    VilkårResultatBuilder vurderSøknadsfrist(BehandlingReferanse referanse, VilkårResultatBuilder vilkårResultatBuilder);
}
