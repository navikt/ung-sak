package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OMPVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();
    private VilkårUtleder vilkårUtleder;
    private SøktePerioder søktePerioder;
    private NulledePerioder nulledePerioder;
    private VilkårResultatRepository vilkårResultatRepository;

    OMPVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public OMPVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef("OMP") VilkårUtleder vilkårUtleder, OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository,
                                                  VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårUtleder = vilkårUtleder;
        søktePerioder = new SøktePerioder(omsorgspengerGrunnlagRepository);
        nulledePerioder = new NulledePerioder(omsorgspengerGrunnlagRepository);
        this.vilkårResultatRepository = vilkårResultatRepository;

        var maksSøktePeriode = new MaksSøktePeriode(omsorgspengerGrunnlagRepository);
        vilkårsPeriodisering.put(VilkårType.MEDLEMSKAPSVILKÅRET, maksSøktePeriode);
        vilkårsPeriodisering.put(VilkårType.OPPTJENINGSVILKÅRET, søktePerioder);
        vilkårsPeriodisering.put(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, søktePerioder); // Støtter da bare et skjæringstidspunkt per behandling
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles(Long behandlingId) {
        return nulledePerioder.utledPeriode(behandlingId);
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var perioder = utledPeriode(behandlingId, vilkårType);
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        if (vilkårene.isPresent()) {
            return utledVilkårsPerioderFraPerioderTilVurdering(vilkårene.get(), perioder);
        }
        return perioder;
    }

    private NavigableSet<DatoIntervallEntitet> utledVilkårsPerioderFraPerioderTilVurdering(Vilkår vilkår, Set<DatoIntervallEntitet> perioder) {
        return vilkår.getPerioder()
            .stream()
            .filter(it -> perioder.stream().anyMatch(p -> it.getPeriode().overlapper(p)))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
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
        return 0;
    }

    private NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId, VilkårType vilkår) {
        return vilkårsPeriodisering.getOrDefault(vilkår, søktePerioder).utledPeriode(behandlingId);
    }

    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return new PåTversAvHelgErKantIKantVurderer();
    }
}
