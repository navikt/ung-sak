package no.nav.k9.sak.inngangsvilkår.opptjeningsperiode;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;

public interface OpptjeningsperiodeVilkårTjeneste {

    // Takler behandlingreferanse som ikke har satt skjæringstidspunkt
    VilkårData vurderOpptjeningsperiodeVilkår(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode);
}
