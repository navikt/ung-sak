package no.nav.ung.sak.domene.behandling.steg.inngangsvilkår;

import java.util.List;
import java.util.NavigableSet;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public interface InngangsvilkårSteg extends BehandlingSteg {

    /** Vilkår håndtert (vurdert) i dette steget. */
    List<VilkårType> vilkårHåndtertAvSteg();

    NavigableSet<DatoIntervallEntitet> perioderTilVurdering(Long behandlingId, VilkårType vilkårType);
}
