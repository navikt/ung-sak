package no.nav.k9.sak.perioder;

import java.util.Map;
import java.util.NavigableSet;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public interface VilkårsPerioderTilVurderingTjeneste {
    NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType);

    Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utled(Long behandlingId);

    int maksMellomliggendePeriodeAvstand();

    default KantIKantVurderer getKantIKantVurderer() {
        return new DefaultKantIKantVurderer();
    }
}
