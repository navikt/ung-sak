package no.nav.ung.sak.vilkår;

import no.nav.ung.sak.behandling.BehandlingReferanse;

public interface VilkårUtleder {
    UtledeteVilkår utledVilkår(BehandlingReferanse referanse);
}
