package no.nav.ung.ytelse.aktivitetspenger.del1;

import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriodeRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.tid.TidslinjeUtil;
import no.nav.ung.sak.vilkår.VilkårUtleder;
import no.nav.ung.sak.vilkår.UtledeteVilkår;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.AKTIVITETSPENGER;

@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@BehandlingTypeRef(BehandlingType.AKTIVITETSPENGER_DEL_1)
public class AktivitetspengerDel1VilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private AktivitetspengerSøktPeriodeRepository aktivitetspengerSøktPeriodeRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårUtleder inngangsvilkårUtleder;

    AktivitetspengerDel1VilkårsPerioderTilVurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerDel1VilkårsPerioderTilVurderingTjeneste(
        AktivitetspengerSøktPeriodeRepository aktivitetspengerSøktPeriodeRepository,
        VilkårResultatRepository vilkårResultatRepository,
        @FagsakYtelseTypeRef(AKTIVITETSPENGER) @BehandlingTypeRef(BehandlingType.AKTIVITETSPENGER_DEL_1) VilkårUtleder inngangsvilkårUtleder) {
        this.aktivitetspengerSøktPeriodeRepository = aktivitetspengerSøktPeriodeRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inngangsvilkårUtleder = inngangsvilkårUtleder;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        if (vilkårene.isPresent()) {
            return vilkårene.filter(it -> it.getVilkårType().equals(vilkårType))
                .map(Vilkår::getPerioder)
                .stream()
                .flatMap(Collection::stream)
                .map(VilkårPeriode::getPeriode)
                .collect(Collectors.toCollection(TreeSet::new));
        }
        return TidslinjeUtil.tilDatoIntervallEntiteter(aktivitetspengerSøktPeriodeRepository.hentSøktePerioderTidslinje(behandlingId));
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        NavigableSet<DatoIntervallEntitet> perioder = utledPerioderFraRelevanteEndringer(behandlingId);
        UtledeteVilkår utledeteVilkår = inngangsvilkårUtleder.utledVilkår(null);
        return utledeteVilkår.getAlleAvklarte().stream()
            .collect(Collectors.toMap(vilkårtype -> vilkårtype, _ -> perioder));
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }


    @Override
    public Set<VilkårType> definerendeVilkår() {
        //FIXME AKT. Dette er sannsynligvis ikke riktig vilkår (spesielt ikke dersom vilkåret flyttes til del2)
        return Set.of(VilkårType.ALDERSVILKÅR);
    }

    /**
     * Finner perioder som vurderes.
     * <p>
     * Endringer som medfører at perioden vurderes er
     * - Nye/endrede søknadsperioder fra bruker
     *
     * @param behandlingId BehandlingId
     * @return Perioder som vurderes
     */
    private NavigableSet<DatoIntervallEntitet> utledPerioderFraRelevanteEndringer(Long behandlingId) {
        return TidslinjeUtil.tilDatoIntervallEntiteter(aktivitetspengerSøktPeriodeRepository.hentSøktePerioderTidslinje(behandlingId));
    }
}
