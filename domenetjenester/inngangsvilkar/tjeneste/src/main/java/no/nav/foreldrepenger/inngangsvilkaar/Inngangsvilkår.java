package no.nav.foreldrepenger.inngangsvilkaar;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface Inngangsvilkår {

    /**
     * Vurder vilkår og returner utfall
     *
     * @param ref     - med grunnlag som skal vurderes
     * @param periode
     * @return {@link VilkårData} som beskriver utfall
     */
    VilkårData vurderVilkår(BehandlingReferanse ref, DatoIntervallEntitet periode);

}
