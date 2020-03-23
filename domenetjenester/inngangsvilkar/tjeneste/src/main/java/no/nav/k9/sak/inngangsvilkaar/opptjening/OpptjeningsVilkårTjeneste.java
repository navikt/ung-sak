package no.nav.k9.sak.inngangsvilkaar.opptjening;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkaar.VilkårData;

public interface OpptjeningsVilkårTjeneste  {

    VilkårData vurderOpptjeningsVilkår(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode);
}
