package no.nav.foreldrepenger.inngangsvilkaar.opptjening;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface OpptjeningsVilkårTjeneste  {

    VilkårData vurderOpptjeningsVilkår(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode);
}
