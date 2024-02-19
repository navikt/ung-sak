package no.nav.k9.sak.ytelse.unntaksbehandling.vilkår;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;

@RequestScoped
public class UnntaksbehandlingVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private FagsakPeriode fagsakPeriode;
    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();

    UnntaksbehandlingVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public UnntaksbehandlingVilkårsPerioderTilVurderingTjeneste(BehandlingRepository behandlingRepository) {
        this.fagsakPeriode = new FagsakPeriode(behandlingRepository);
        vilkårsPeriodisering.put(VilkårType.K9_VILKÅRET, fagsakPeriode);
    }

    @Override
    public KantIKantVurderer getKantIKantVurderer() {
            return new DefaultKantIKantVurderer();
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        return utledPeriode(behandlingId, vilkårType);
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        final var vilkårPeriodeSet = new HashMap<VilkårType, NavigableSet<DatoIntervallEntitet>>();
        UtledeteVilkår utledeteVilkår = getVilkårUtleder().utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledPeriode(behandlingId, vilkår)));

        return vilkårPeriodeSet;
    }

    private VilkårUtleder getVilkårUtleder() {
        return behandling -> new UtledeteVilkår(null, List.of(VilkårType.K9_VILKÅRET));
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }

    private NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId, VilkårType vilkår) {
        return vilkårsPeriodisering.getOrDefault(vilkår, fagsakPeriode).utledPeriode(behandlingId);
    }

}
