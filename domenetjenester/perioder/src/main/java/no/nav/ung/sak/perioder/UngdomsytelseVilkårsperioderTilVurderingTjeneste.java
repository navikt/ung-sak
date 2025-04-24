package no.nav.ung.sak.perioder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UtledPeriodeTilVurderingFraUngdomsprogram;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.vilkår.InngangsvilkårUtleder;
import no.nav.ung.sak.vilkår.UtledeteVilkår;

import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
@ApplicationScoped
public class UngdomsytelseVilkårsperioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private InngangsvilkårUtleder inngangsvilkårUtleder;

    private UngdomsytelseSøknadsperiodeTjeneste ungdomsytelseSøknadsperiodeTjeneste;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private UtledPeriodeTilVurderingFraUngdomsprogram utledPeriodeTilVurderingFraUngdomsprogram;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;


    UngdomsytelseVilkårsperioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public UngdomsytelseVilkårsperioderTilVurderingTjeneste(
        @FagsakYtelseTypeRef(UNGDOMSYTELSE) InngangsvilkårUtleder inngangsvilkårUtleder,
        UngdomsytelseSøknadsperiodeTjeneste ungdomsytelseSøknadsperiodeTjeneste,
        UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
        UtledPeriodeTilVurderingFraUngdomsprogram utledPeriodeTilVurderingFraUngdomsprogram,
        ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
        VilkårResultatRepository vilkårResultatRepository, BehandlingRepository behandlingRepository) {
        this.inngangsvilkårUtleder = inngangsvilkårUtleder;
        this.ungdomsytelseSøknadsperiodeTjeneste = ungdomsytelseSøknadsperiodeTjeneste;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.utledPeriodeTilVurderingFraUngdomsprogram = utledPeriodeTilVurderingFraUngdomsprogram;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
    }


    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return new DefaultKantIKantVurderer();
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        if (vilkårene.isPresent()) {
            final var perioder = utledPerioderFraRelevanteEndringer(behandlingId);
            return vilkårene.filter(it -> it.getVilkårType().equals(vilkårType))
                .map(Vilkår::getPerioder)
                .stream()
                .flatMap(Collection::stream)
                .map(VilkårPeriode::getPeriode)
                .filter(p -> perioder.stream().anyMatch(p::overlapper))
                .collect(Collectors.toCollection(TreeSet::new));
        }
        return TidslinjeUtil.tilDatoIntervallEntiteter(ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId));
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        final var vilkårPeriodeSet = new HashMap<VilkårType, NavigableSet<DatoIntervallEntitet>>();
        UtledeteVilkår utledeteVilkår = inngangsvilkårUtleder.utledVilkår(null);
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        final var fagsakperiode = behandling.getFagsak().getPeriode();
        var programperioder = ungdomsprogramPeriodeTjeneste.finnEndretPeriodeTidslinjeFraOriginal(BehandlingReferanse.fra(behandling))
            .intersection(fagsakperiode.toLocalDateInterval()).compress();
        utledeteVilkår.getAlleAvklarte()
                .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, TidslinjeUtil.tilDatoIntervallEntiteter(programperioder)));

        return vilkårPeriodeSet;
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        return Set.of(VilkårType.UNGDOMSPROGRAMVILKÅRET);
    }



    /**
     * Finner perioder som vurderes.
     * <p>
     * Endringer som medfører at perioden vurderes er
     * - Nye søknadsperioder fra bruker
     * - Endringer i ungdomsprogram i perioder som er søkt om
     *
     * @param behandlingId BehandlingId
     * @return Perioder som vurderes
     */
    private NavigableSet<DatoIntervallEntitet> utledPerioderFraRelevanteEndringer(Long behandlingId) {
        var tidslinjeForRelevanteEndringerIUngdomsprogram = utledPeriodeTilVurderingFraUngdomsprogram.finnTidslinje(behandlingId);
        var relevantePerioderTidslinje = ungdomsytelseSøknadsperiodeTjeneste.utledTidslinje(behandlingId);
        final var tidslinjeFraTrigger = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId).mapValue(it -> true);
        var tidslinjeTilVurdering = tidslinjeForRelevanteEndringerIUngdomsprogram.crossJoin(relevantePerioderTidslinje).crossJoin(tidslinjeFraTrigger);

        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinjeTilVurdering.compress());
    }

}
