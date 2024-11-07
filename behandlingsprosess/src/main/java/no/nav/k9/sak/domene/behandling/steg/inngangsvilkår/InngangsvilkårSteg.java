package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår;

import java.util.List;
import java.util.NavigableSet;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface InngangsvilkårSteg extends BehandlingSteg {

    /** Vilkår håndtert (vurdert) i dette steget. */
    List<VilkårType> vilkårHåndtertAvSteg();

    NavigableSet<DatoIntervallEntitet> perioderTilVurdering(Long behandlingId, VilkårType vilkårType);
}
