package no.nav.k9.sak.ytelse.frisinn.vilkår;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();
    private MaksSøktePeriode maksSøktePeriode;
    private VilkårUtleder vilkårUtleder;

    FrisinnVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public FrisinnVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef("FRISINN") VilkårUtleder vilkårUtleder, UttakRepository uttakRepository, BehandlingRepository behandlingRepository) {
        this.maksSøktePeriode = new MaksSøktePeriode(uttakRepository);
        final var beregningPeriode = new Søknadsperioder(uttakRepository);
        vilkårsPeriodisering.put(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, beregningPeriode);
        this.vilkårUtleder = vilkårUtleder;
    }

    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return new IkkeKantIKantVurderer();
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        return utledPeriode(behandlingId, vilkårType);
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utled(Long behandlingId) {
        final var vilkårPeriodeSet = new HashMap<VilkårType, NavigableSet<DatoIntervallEntitet>>();
        UtledeteVilkår utledeteVilkår = vilkårUtleder.utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledPeriode(behandlingId, vilkår)));

        return vilkårPeriodeSet;
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 365;
    }

    private NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId, VilkårType vilkår) {
        return vilkårsPeriodisering.getOrDefault(vilkår, maksSøktePeriode).utledPeriode(behandlingId);
    }

}
