package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableSet;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPerioderTilVurderingTjeneste;

@FagsakYtelseTypeRef
@ApplicationScoped
public class TestVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {


    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        return Collections.emptyNavigableSet();
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utled(Long behandlingId) {
        return Map.of();
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }
}
