package no.nav.k9.sak.perioder;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;

public interface SøknadsfristTjeneste {

    VilkårResultatBuilder vurderSøknadsfrist(BehandlingReferanse referanse, VilkårResultatBuilder vilkårResultatBuilder);
}
