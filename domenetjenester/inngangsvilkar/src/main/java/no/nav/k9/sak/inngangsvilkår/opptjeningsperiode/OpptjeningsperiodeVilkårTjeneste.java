package no.nav.k9.sak.inngangsvilkår.opptjeningsperiode;

import java.util.Collection;
import java.util.NavigableMap;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;

public interface OpptjeningsperiodeVilkårTjeneste {

    // Takler behandlingreferanse som ikke har satt skjæringstidspunkt
    NavigableMap<DatoIntervallEntitet, VilkårData> vurderOpptjeningsperiodeVilkår(BehandlingReferanse behandlingReferanse, Collection<DatoIntervallEntitet> periode);
}
