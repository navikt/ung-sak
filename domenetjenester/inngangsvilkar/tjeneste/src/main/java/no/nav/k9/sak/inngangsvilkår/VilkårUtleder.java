package no.nav.k9.sak.inngangsvilkår;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface VilkårUtleder {
    UtledeteVilkår utledVilkår(Behandling behandling);
}
