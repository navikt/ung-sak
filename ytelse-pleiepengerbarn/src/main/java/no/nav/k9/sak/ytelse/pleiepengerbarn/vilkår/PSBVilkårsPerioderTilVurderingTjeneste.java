package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
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
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperioderHolder;

@FagsakYtelseTypeRef("PSB")
@BehandlingTypeRef
@ApplicationScoped
public class PSBVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private final PåTversAvHelgErKantIKantVurderer erKantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();

    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();
    private VilkårUtleder vilkårUtleder;
    private SøktePerioder søktePerioder;
    private VilkårResultatRepository vilkårResultatRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private BehandlingRepository behandlingRepository;
    private SykdomGrunnlagRepository sykdomGrunnlagRepository;

    PSBVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public PSBVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef("PSB") VilkårUtleder vilkårUtleder,
                                                  SøknadsperiodeRepository søknadsperiodeRepository,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  BehandlingRepository behandlingRepository, SykdomGrunnlagRepository sykdomGrunnlagRepository) {
        this.vilkårUtleder = vilkårUtleder;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.behandlingRepository = behandlingRepository;
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
        var maksSøktePeriode = new MaksSøktePeriode(this.søknadsperiodeRepository);
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
        AktørId pleietrengende = behandlingRepository.hentBehandling(referanse.getBehandlingUuid()).getFagsak().getPleietrengendeAktørId();

        final Optional<SykdomGrunnlagBehandling> forrigeGrunnlagBehandling = sykdomGrunnlagRepository.hentGrunnlagFraForrigeBehandling(referanse.getSaksnummer(), referanse.getBehandlingUuid());
        LocalDateTimeline<SykdomSamletVurdering> forrigeBehandlingTidslinje;

        if (forrigeGrunnlagBehandling.isPresent()) {
            final SykdomGrunnlag forrigeGrunnlag = forrigeGrunnlagBehandling.get().getGrunnlag();
            forrigeBehandlingTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(forrigeGrunnlag);
        } else {
            forrigeBehandlingTidslinje = LocalDateTimeline.EMPTY_TIMELINE;
        }

        var perioder = utled(referanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var vurderingsperioderTimeline = SykdomUtils.toLocalDateTimeline(perioder);

        SykdomGrunnlag utledetGrunnlag = sykdomGrunnlagRepository.utledGrunnlag(referanse.getSaksnummer(), referanse.getBehandlingUuid(), pleietrengende, SykdomUtils.toPeriodeList(perioder));
        LocalDateTimeline<SykdomSamletVurdering> nyBehandlingTidslinje = SykdomSamletVurdering.grunnlagTilTidslinje(utledetGrunnlag);

        LocalDateTimeline<Boolean> endringerSidenForrigeBehandling = SykdomSamletVurdering.finnGrunnlagsforskjeller(forrigeBehandlingTidslinje, nyBehandlingTidslinje);

        LocalDateTimeline<Boolean> utvidedePerioder = SykdomUtils.kunPerioderSomIkkeFinnesI(endringerSidenForrigeBehandling, vurderingsperioderTimeline);

        return new TreeSet<>(utvidedePerioder.stream()
            .map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom()))
            .collect(Collectors.toList()));
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledFullstendigePerioder(Long behandlingId) {
        // TODO: Ignorerer rekkefølge for nå, blir først en utfordring når brukeren kan "trekke" perioder ved søknad
        var alleSøknadsperioder = søknadsperiodeRepository.hentGrunnlag(behandlingId)
            .map(SøknadsperiodeGrunnlag::getOppgitteSøknadsperioder)
            .map(SøknadsperioderHolder::getPerioder)
            .orElse(Set.of());

        return søktePerioder.utledVurderingsperioderFraSøknadsperioder(alleSøknadsperioder);
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
