package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@FagsakYtelseTypeRef
@ApplicationScoped
public class TestVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {


    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        return utled(behandlingId).getOrDefault(vilkårType, new TreeSet<>());
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utled(Long behandlingId) {
        return Map.of(VilkårType.MEDLEMSKAPSVILKÅRET, new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusYears(3)))));
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }
}
