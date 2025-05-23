package no.nav.ung.sak.perioder;

import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef
@BehandlingTypeRef
@ApplicationScoped
public class FullVilkårsperiodeUtleder implements EndretUtbetalingPeriodeutleder {

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;


    public FullVilkårsperiodeUtleder() {
    }

    @Inject
    public FullVilkårsperiodeUtleder(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester) {
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode) {
        return new TreeSet<>(Set.of(periode));
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse) {
        var periodeTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
        return periodeTjeneste.utled(behandlingReferanse.getBehandlingId(), VilkårType.UNGDOMSPROGRAMVILKÅRET).stream()
            .flatMap(p -> utledPerioder(behandlingReferanse, p).stream())
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
