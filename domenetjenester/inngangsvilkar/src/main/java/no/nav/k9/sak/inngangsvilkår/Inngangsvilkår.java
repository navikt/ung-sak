package no.nav.k9.sak.inngangsvilkår;

import java.util.Collection;
import java.util.NavigableMap;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface Inngangsvilkår {

    /**
     * Vurder vilkår og returner utfall
     *
     * @param ref - med grunnlag som skal vurderes
     * @param periode
     * @return {@link VilkårData} som beskriver utfall per angitt periode
     */
    NavigableMap<DatoIntervallEntitet, VilkårData> vurderVilkår(BehandlingReferanse ref, Collection<DatoIntervallEntitet> periode);

}
