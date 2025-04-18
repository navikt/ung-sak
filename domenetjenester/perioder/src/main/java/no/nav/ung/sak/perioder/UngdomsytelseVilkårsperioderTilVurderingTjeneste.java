package no.nav.ung.sak.perioder;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UtledPeriodeTilVurderingFraUngdomsprogram;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.vilkår.InngangsvilkårUtleder;
import no.nav.ung.sak.vilkår.UtledeteVilkår;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
@ApplicationScoped
public class UngdomsytelseVilkårsperioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private InngangsvilkårUtleder inngangsvilkårUtleder;

    private UngdomsytelseSøknadsperiodeTjeneste ungdomsytelseSøknadsperiodeTjeneste;
    private UtledPeriodeTilVurderingFraUngdomsprogram utledPeriodeTilVurderingFraUngdomsprogram;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;
    private VilkårResultatRepository vilkårResultatRepository;


    UngdomsytelseVilkårsperioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public UngdomsytelseVilkårsperioderTilVurderingTjeneste(
        @FagsakYtelseTypeRef(UNGDOMSYTELSE) InngangsvilkårUtleder inngangsvilkårUtleder,
        UngdomsytelseSøknadsperiodeTjeneste ungdomsytelseSøknadsperiodeTjeneste,
        UtledPeriodeTilVurderingFraUngdomsprogram utledPeriodeTilVurderingFraUngdomsprogram,
        ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
        MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder, VilkårResultatRepository vilkårResultatRepository) {
        this.inngangsvilkårUtleder = inngangsvilkårUtleder;
        this.ungdomsytelseSøknadsperiodeTjeneste = ungdomsytelseSøknadsperiodeTjeneste;
        this.utledPeriodeTilVurderingFraUngdomsprogram = utledPeriodeTilVurderingFraUngdomsprogram;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
        this.vilkårResultatRepository = vilkårResultatRepository;
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
        return ungdomsytelseSøknadsperiodeTjeneste.utledPeriode(behandlingId);
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        final var vilkårPeriodeSet = new HashMap<VilkårType, NavigableSet<DatoIntervallEntitet>>();
        UtledeteVilkår utledeteVilkår = inngangsvilkårUtleder.utledVilkår(null);
        var søktePerioder = ungdomsytelseSøknadsperiodeTjeneste.utledPeriode(behandlingId);
        utledeteVilkår.getAlleAvklarte()
                .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, søktePerioder));

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


        final var månedssegmenterTilVurdering = månedsvisTidslinjeUtleder.periodiserMånedsvis(behandlingId)
            .stream().filter(s -> !tidslinjeTilVurdering.intersection(s.getLocalDateInterval()).isEmpty())
            .toList();

        return TidslinjeUtil.tilDatoIntervallEntiteter(new LocalDateTimeline<>(månedssegmenterTilVurdering).compress());
    }

}
