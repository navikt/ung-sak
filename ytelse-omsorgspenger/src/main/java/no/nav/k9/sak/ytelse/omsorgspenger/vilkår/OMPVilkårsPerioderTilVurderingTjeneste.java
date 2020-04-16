package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OMPVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();
    private VilkårUtleder vilkårUtleder;
    private SøktePerioder søktePerioder;

    OMPVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public OMPVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef("OMP") VilkårUtleder vilkårUtleder, OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository) {
        this.vilkårUtleder = vilkårUtleder;
        søktePerioder = new SøktePerioder(omsorgspengerGrunnlagRepository);

        vilkårsPeriodisering.put(VilkårType.OPPTJENINGSVILKÅRET, søktePerioder);
        vilkårsPeriodisering.put(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, søktePerioder);
    }

    @Override
    public Set<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        return utledPeriode(behandlingId, vilkårType);
    }

    @Override
    public Map<VilkårType, Set<DatoIntervallEntitet>> utled(Long behandlingId) {
        final var vilkårPeriodeSet = new HashMap<VilkårType, Set<DatoIntervallEntitet>>();
        UtledeteVilkår utledeteVilkår = vilkårUtleder.utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledPeriode(behandlingId, vilkår)));

        return vilkårPeriodeSet;
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }

    private Set<DatoIntervallEntitet> utledPeriode(Long behandlingId, VilkårType vilkår) {
        return vilkårsPeriodisering.getOrDefault(vilkår, søktePerioder).utledPeriode(behandlingId);
    }

}
