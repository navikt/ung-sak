package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;

@FagsakYtelseTypeRef("PSB")
@BehandlingTypeRef
@ApplicationScoped
public class PSBVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private final PåTversAvHelgErKantIKantVurderer erKantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();

    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();
    private VilkårUtleder vilkårUtleder;
    private SøktePerioder søktePerioder;
    private VilkårResultatRepository vilkårResultatRepository;

    PSBVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public PSBVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef("PSB") VilkårUtleder vilkårUtleder,
                                                  @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste,
                                                  SøknadsperiodeRepository søknadsperiodeRepository,
                                                  VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårUtleder = vilkårUtleder;
        var maksSøktePeriode = new MaksSøktePeriode(søknadsperiodeRepository);
        this.vilkårResultatRepository = vilkårResultatRepository;

        søktePerioder = new SøktePerioder(søknadsperiodeRepository);

        vilkårsPeriodisering.put(VilkårType.MEDLEMSKAPSVILKÅRET, maksSøktePeriode);
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var perioder = utledPeriode(behandlingId, vilkårType);
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        if (vilkårene.isPresent()) {
            return utledVilkårsPerioderFraPerioderTilVurdering(vilkårene.get(), perioder);
        }
        return utledPeriode(behandlingId, vilkårType);
    }

    private NavigableSet<DatoIntervallEntitet> utledVilkårsPerioderFraPerioderTilVurdering(Vilkår vilkår, Set<DatoIntervallEntitet> perioder) {
        return vilkår.getPerioder()
            .stream()
            .map(VilkårPeriode::getPeriode)
            .filter(datoIntervallEntitet -> perioder.stream().anyMatch(datoIntervallEntitet::overlapper))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utled(Long behandlingId) {
        final var vilkårPeriodeSet = new EnumMap<VilkårType, NavigableSet<DatoIntervallEntitet>>(VilkårType.class);
        UtledeteVilkår utledeteVilkår = vilkårUtleder.utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledPeriode(behandlingId, vilkår)));

        return vilkårPeriodeSet;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledUtvidetRevurderingPerioder(BehandlingReferanse referanse) {
        // TODO: Returnere liste av alle perioder på utsiden av revurderingsperiode som har blitt endret.
        return new TreeSet<>();
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
        return erKantIKantVurderer;
    }

}
