package no.nav.k9.sak.ytelse.unntaksbehandling.vilkår;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

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
public class UNNTVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private FagsakPeriode fagsakPeriode;
    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();

    UNNTVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public UNNTVilkårsPerioderTilVurderingTjeneste(BehandlingRepository behandlingRepository) {
        this.fagsakPeriode = new FagsakPeriode(behandlingRepository);;
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
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utled(Long behandlingId) {
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
