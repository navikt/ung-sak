package no.nav.k9.sak.inngangsvilkår;

import no.nav.k9.sak.behandling.BehandlingReferanse;

public interface VilkårUtleder {
    UtledeteVilkår utledVilkår(BehandlingReferanse referanse);
}
