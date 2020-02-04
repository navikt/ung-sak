package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.vilkår.VilkårType;

public interface InngangsvilkårSteg extends BehandlingSteg {

    /** Vilkår håndtert (vurdert) i dette steget. */
    List<VilkårType> vilkårHåndtertAvSteg();

    List<DatoIntervallEntitet> perioderTilVurdering(Long behandlingId, VilkårType vilkårType);
}
