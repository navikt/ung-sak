package no.nav.k9.sak.inngangsvilkår.opptjening;

import java.util.Collection;
import java.util.NavigableMap;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;

public interface OpptjeningsVilkårTjeneste {

    NavigableMap<DatoIntervallEntitet, VilkårData> vurderOpptjeningsVilkår(BehandlingReferanse behandlingReferanse, Collection<DatoIntervallEntitet> perioder);
}
