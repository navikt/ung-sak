package no.nav.ung.sak.perioder;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.vilkår.InngangsvilkårUtleder;
import no.nav.ung.sak.vilkår.UtledeteVilkår;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
@ApplicationScoped
public class UngdomsytelseVilkårsperioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private InngangsvilkårUtleder inngangsvilkårUtleder;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private BehandlingRepository behandlingRepository;
    private UngdomsytelseSøknadsperiodeTjeneste ungdomsytelseSøknadsperiodeTjeneste;

    UngdomsytelseVilkårsperioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public UngdomsytelseVilkårsperioderTilVurderingTjeneste(
        @FagsakYtelseTypeRef(UNGDOMSYTELSE) InngangsvilkårUtleder inngangsvilkårUtleder, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
        BehandlingRepository behandlingRepository, UngdomsytelseSøknadsperiodeTjeneste ungdomsytelseSøknadsperiodeTjeneste) {
        this.inngangsvilkårUtleder = inngangsvilkårUtleder;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.ungdomsytelseSøknadsperiodeTjeneste = ungdomsytelseSøknadsperiodeTjeneste;
    }


    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return new DefaultKantIKantVurderer();
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        return utledPeriode(behandlingId);
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

    /** Finner perioder som vurderes.
     * <p>
     * Endringer som medfører at perioden vurderes er
     * - Nye søknadsperioder fra bruker
     * - Endringer i ungdomsprogram i perioder som er søkt om
     *
     * @param behandlingId BehandlingId
     * @return Perioder som vurderes
     */
    private NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var tidslinjeForRelevanteEndringerIUngdomsprogram = finnRelevanteEndringerIUngdomsprogram(behandlingId);
        var relevantePerioderTidslinje = TidslinjeUtil.tilTidslinje(ungdomsytelseSøknadsperiodeTjeneste.utledPeriode(behandlingId));
        var tidslinjeTilVurdering = tidslinjeForRelevanteEndringerIUngdomsprogram.crossJoin(relevantePerioderTidslinje);
        return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinjeTilVurdering);
    }

    private LocalDateTimeline<Boolean> finnRelevanteEndringerIUngdomsprogram(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        // Finner alle søknadsperioder på saken
        var alleSøknadsperioder = ungdomsytelseSøknadsperiodeTjeneste.utledFullstendigPeriode(behandlingId);
        var søknadsperiodeTidslinje = TidslinjeUtil.tilTidslinje(alleSøknadsperioder);

        // Finner endringer i perioder der bruker er meldt inn i ungdomsprogram
        var ungdomsprogramEndretTidslinje = ungdomsprogramPeriodeTjeneste.finnEndretPeriodeTidslinje(BehandlingReferanse.fra(behandling));

        // Kun perioder med endringer som overlapper med søkte perioder tas hensyn til
        var endretTidslinje = ungdomsprogramEndretTidslinje.intersection(søknadsperiodeTidslinje);
        return endretTidslinje;
    }

}
