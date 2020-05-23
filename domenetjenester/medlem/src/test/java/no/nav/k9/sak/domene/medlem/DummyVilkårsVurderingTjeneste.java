package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.RequestScoped;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@FagsakYtelseTypeRef
@RequestScoped
public class DummyVilkårsVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        return new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusYears(3))));
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
