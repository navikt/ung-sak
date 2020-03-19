package no.nav.foreldrepenger.inngangsvilkaar.impl;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;

public interface VilkårUtleder {
    UtledeteVilkår utledVilkår(Behandling behandling);
}
