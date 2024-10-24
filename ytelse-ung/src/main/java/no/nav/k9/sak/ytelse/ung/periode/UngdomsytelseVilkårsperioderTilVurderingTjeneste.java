package no.nav.k9.sak.ytelse.ung.periode;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.ung.inngangsvilkår.InngangsvilkårUtleder;
import no.nav.k9.sak.ytelse.ung.søknadsperioder.UngdomsytelseSøknadsperiodeTjeneste;

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

    private NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        return finnSøktePerioderOgEndringerIUngdomsprogram(behandlingId);
    }

    private NavigableSet<DatoIntervallEntitet> finnSøktePerioderOgEndringerIUngdomsprogram(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var søknadsperioder = ungdomsytelseSøknadsperiodeTjeneste.utledPeriode(behandlingId);
        var søknadsperiodeTidslinje = TidslinjeUtil.tilTidslinje(søknadsperioder);
        var ungdomsprogramEndretTidslinje = ungdomsprogramPeriodeTjeneste.finnEndretPeriodeTidslinje(BehandlingReferanse.fra(behandling), getKantIKantVurderer());
        var endretTidslinje = ungdomsprogramEndretTidslinje.crossJoin(søknadsperiodeTidslinje);
        return TidslinjeUtil.tilDatoIntervallEntiteter(endretTidslinje);
    }

}
