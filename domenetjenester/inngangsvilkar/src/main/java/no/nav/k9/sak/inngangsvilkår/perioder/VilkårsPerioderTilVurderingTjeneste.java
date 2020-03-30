package no.nav.k9.sak.inngangsvilkår.perioder;

import java.util.Map;
import java.util.Set;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface VilkårsPerioderTilVurderingTjeneste {
    Set<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType);

    Map<VilkårType, Set<DatoIntervallEntitet>> utled(Long behandlingId);

    int maksMellomliggendePeriodeAvstand();
}
