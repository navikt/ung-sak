package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.alenemidlertidig;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.UtvidetRettSøknadPerioder;

@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
@BehandlingTypeRef
@Dependent
public class MidlertidigAleneVilkårsVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private UtvidetRettSøknadPerioder søktePerioder;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårUtleder vilkårUtleder;

    @Inject
    public MidlertidigAleneVilkårsVurderingTjeneste(@FagsakYtelseTypeRef(OMSORGSPENGER_MA) VilkårUtleder vilkårUtleder,
                                                    VilkårResultatRepository vilkårResultatRepository,
                                                    SøknadRepository søknadRepository) {
        this.vilkårUtleder = vilkårUtleder;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søktePerioder = new UtvidetRettSøknadPerioder(søknadRepository);
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles(Long behandlingId) {
        return new TreeSet<>();
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledFullstendigePerioder(Long behandlingId) {
        var søknadsperioder = søktePerioder.utledPeriode(behandlingId);
        var maksPeriode = utledMaksPeriode(søknadsperioder);
        return Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(maksPeriode)));
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var optVilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (optVilkårene.isPresent()) {
            var vilkårTidslinje = optVilkårene.get().getVilkårTimeline(vilkårType);
            if (vilkårTidslinje.isEmpty()) {
                return Collections.emptyNavigableSet();
            }
            return TidslinjeUtil.tilDatoIntervallEntiteter(vilkårTidslinje);
        } else {
            // default til 'fullstedige' perioder hvis vilkår ikke angitt.
            return utledFullstendigePerioder(behandlingId);
        }
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        final var vilkårPeriodeSet = new EnumMap<VilkårType, NavigableSet<DatoIntervallEntitet>>(VilkårType.class);
        UtledeteVilkår utledeteVilkår = vilkårUtleder.utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledFullstendigePerioder(behandlingId)));

        return vilkårPeriodeSet;
    }

    DatoIntervallEntitet utledMaksPeriode(NavigableSet<DatoIntervallEntitet> søktePerioder) {
        // 1. jan minst 3 år før søknad sendt inn (spesielle særtilfeller tillater at et går an å sette tilbake it itid
        var førsteSøktePeriode = søktePerioder.first();

        var mindato = førsteSøktePeriode.getFomDato().minusYears(3).withMonth(1).withDayOfMonth(1);
        var maksdato = Tid.TIDENES_ENDE; // TODO: Sjekk mot personopplysninger?
        return DatoIntervallEntitet.fraOgMedTilOgMed(mindato, maksdato);
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return Integer.MAX_VALUE;
    }

    public DatoIntervallEntitet utledMaksPeriode(DatoIntervallEntitet periode) {
        return utledMaksPeriode(new TreeSet<>(Set.of(periode)));
    }

}
