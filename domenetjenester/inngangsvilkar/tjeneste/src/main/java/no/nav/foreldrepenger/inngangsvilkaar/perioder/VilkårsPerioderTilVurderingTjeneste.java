package no.nav.foreldrepenger.inngangsvilkaar.perioder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.impl.DefaultVilkårUtleder;
import no.nav.foreldrepenger.inngangsvilkaar.impl.UtledeteVilkår;
import no.nav.k9.kodeverk.vilkår.VilkårType;

@ApplicationScoped
public class VilkårsPerioderTilVurderingTjeneste {

    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();
    private MaksSøktePeriode maksSøktePeriode;

    VilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public VilkårsPerioderTilVurderingTjeneste(FordelingRepository fordelingRepository) {
        this.maksSøktePeriode = new MaksSøktePeriode(fordelingRepository);
        final var søktePerioder = new SøktePerioder(fordelingRepository);

        vilkårsPeriodisering.put(VilkårType.OPPTJENINGSVILKÅRET, søktePerioder);
        vilkårsPeriodisering.put(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, søktePerioder);
    }

    public Set<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        return utledPeriode(behandlingId, vilkårType);
    }

    public Map<VilkårType, Set<DatoIntervallEntitet>> utled(Long behandlingId) {
        final var vilkårPeriodeSet = new HashMap<VilkårType, Set<DatoIntervallEntitet>>();
        UtledeteVilkår utledeteVilkår = new DefaultVilkårUtleder().utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledPeriode(behandlingId, vilkår)));

        return vilkårPeriodeSet;
    }

    private Set<DatoIntervallEntitet> utledPeriode(Long behandlingId, VilkårType vilkår) {
        return vilkårsPeriodisering.getOrDefault(vilkår, maksSøktePeriode).utledPeriode(behandlingId);
    }

}
