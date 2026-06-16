package no.nav.ung.ytelse.aktivitetspenger.del1;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.UtledeteVilkår;
import no.nav.ung.sak.vilkår.VilkårUtleder;
import no.nav.ung.ytelse.aktivitetspenger.perioder.AktivitetspengerSøknadsperiodeTjeneste;

import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.AKTIVITETSPENGER;

@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@BehandlingTypeRef
@ApplicationScoped
public class AktivitetspengerVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private AktivitetspengerSøknadsperiodeTjeneste aktivitetspengerSøknadsperiodeTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårUtleder inngangsvilkårUtleder;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;

    AktivitetspengerVilkårsPerioderTilVurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerVilkårsPerioderTilVurderingTjeneste(
        AktivitetspengerSøknadsperiodeTjeneste aktivitetspengerSøknadsperiodeTjeneste,
        VilkårResultatRepository vilkårResultatRepository,
        @FagsakYtelseTypeRef(AKTIVITETSPENGER) @BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD) VilkårUtleder inngangsvilkårUtleder,
        @FagsakYtelseTypeRef(AKTIVITETSPENGER) ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder) {
        this.aktivitetspengerSøknadsperiodeTjeneste = aktivitetspengerSøknadsperiodeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inngangsvilkårUtleder = inngangsvilkårUtleder;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        if (vilkårene.isPresent()) {
            LocalDateTimeline<Set<BehandlingÅrsakType>> prosesstriggerTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId);

            Set<BehandlingÅrsakType> relevanteÅrsaker = hentRelevanteÅrsaker(vilkårType);
            LocalDateTimeline<Boolean> relevantePerioderTidslinje = prosesstriggerTidslinje
                .filterValue(årsaker -> årsaker.stream().anyMatch(relevanteÅrsaker::contains))
                .mapValue(årsaker -> Boolean.TRUE);

            return vilkårene.filter(it -> it.getVilkårType().equals(vilkårType))
                .map(Vilkår::getPerioder)
                .stream()
                .flatMap(Collection::stream)
                .map(VilkårPeriode::getPeriode)
                .filter(it -> !relevantePerioderTidslinje.intersection(it.toLocalDateInterval()).isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        }
        return aktivitetspengerSøknadsperiodeTjeneste.utledPeriode(behandlingId);
    }

    private Set<BehandlingÅrsakType> hentRelevanteÅrsaker(VilkårType vilkårType) {
        EnumSet<BehandlingÅrsakType> årsaker = EnumSet.of(BehandlingÅrsakType.NY_SØKT_PERIODE);
        if (vilkårType == VilkårType.BOSTEDSVILKÅR) {
            årsaker.add(BehandlingÅrsakType.ENDRET_BOSTED);
        }
        return årsaker;
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
        return aktivitetspengerSøknadsperiodeTjeneste.utledPeriode(behandlingId);
    }
}
