package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.PeriodeMedÅrsak;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriode;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.vilkår.EndringIUttakPeriodeUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.PleietrengendeRevurderingPerioderTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.RevurderingPerioderTjeneste;

public abstract class PleiepengerVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private final PåTversAvHelgErKantIKantVurderer erKantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();
    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering;
    private VilkårUtleder vilkårUtleder;
    private SøktePerioder søktePerioder;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private RevurderingPerioderTjeneste revurderingPerioderTjeneste;
    private PleietrengendeRevurderingPerioderTjeneste pleitrengendeRevurderingPerioderTjeneste;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;

    private EndringIUttakPeriodeUtleder endringIUttakPeriodeUtleder;


    public PleiepengerVilkårsPerioderTilVurderingTjeneste() {
    }

    @Inject
    public PleiepengerVilkårsPerioderTilVurderingTjeneste(VilkårUtleder vilkårUtleder,
                                                          Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering,
                                                          VilkårResultatRepository vilkårResultatRepository,
                                                          BehandlingRepository behandlingRepository,
                                                          RevurderingPerioderTjeneste revurderingPerioderTjeneste,
                                                          PleietrengendeRevurderingPerioderTjeneste pleitrengendeRevurderingPerioderTjeneste,
                                                          SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                          UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                                          EndringIUttakPeriodeUtleder endringIUttakPeriodeUtleder) {
        this.vilkårUtleder = vilkårUtleder;
        this.vilkårsPeriodisering = vilkårsPeriodisering;
        this.behandlingRepository = behandlingRepository;
        this.revurderingPerioderTjeneste = revurderingPerioderTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.pleitrengendeRevurderingPerioderTjeneste = pleitrengendeRevurderingPerioderTjeneste;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        søktePerioder = new SøktePerioder(søknadsperiodeTjeneste);
        this.endringIUttakPeriodeUtleder = endringIUttakPeriodeUtleder;
    }

    @Override
    @WithSpan
    public NavigableSet<DatoIntervallEntitet> utled(@SpanAttribute("behandlingId") Long behandlingId, @SpanAttribute("vilkarType") VilkårType vilkårType) {
        var perioder = utledPeriode(behandlingId, vilkårType);
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        if (vilkårene.isPresent()) {
            return utledVilkårsPerioderFraPerioderTilVurdering(behandlingId, vilkårene.get(), perioder);
        }
        return utledPeriode(behandlingId, vilkårType);
    }

    private NavigableSet<DatoIntervallEntitet> utledVilkårsPerioderFraPerioderTilVurdering(Long behandlingId, Vilkår vilkår, NavigableSet<DatoIntervallEntitet> perioder) {
        var perioderTilVurdering = new TreeSet<>(utledPerioderTilVurderingVedÅHensyntaFullstendigTidslinje(behandlingId, perioder));

        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var referanse = BehandlingReferanse.fra(behandling);
        if (skalVurdereBerørtePerioderPåBarnet(behandling)) {
            var tidslinjeMedÅrsaker = pleitrengendeRevurderingPerioderTjeneste.utledBerørtePerioderPåPleietrengende(referanse, definerendeVilkår());
            perioderTilVurdering.addAll(TidslinjeUtil.tilDatoIntervallEntiteter(tidslinjeMedÅrsaker));
            perioderTilVurdering.addAll(TidslinjeUtil.tilDatoIntervallEntiteter(uttaksendringerSidenForrigeBehandling(referanse)));
        }

        perioderTilVurdering.addAll(revurderingPerioderTjeneste.utledPerioderFraProsessTriggere(referanse));
        perioderTilVurdering.addAll(revurderingPerioderTjeneste.utledPerioderFraInntektsmeldinger(referanse, finnKomprimertePerioderFraKantIKantVurderer(behandling)));
        perioderTilVurdering.addAll(perioderSomSkalTilbakestilles(behandlingId));

        return vilkår.getPerioder()
            .stream()
            .map(VilkårPeriode::getPeriode)
            .filter(datoIntervallEntitet -> perioderTilVurdering.stream().anyMatch(it -> datoIntervallEntitet.overlapper(it.getFomDato().minusDays(1), it.getTomDato().plusDays(1))))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<DatoIntervallEntitet> utledPerioderTilVurderingVedÅHensyntaFullstendigTidslinje(Long behandlingId, NavigableSet<DatoIntervallEntitet> perioder) {
        var datoIntervallEntitets = utledFullstendigePerioder(behandlingId);
        return utledPeriodeEtterHensynÅHaHensyntattFullstendigTidslinje(perioder, datoIntervallEntitets);
    }

    NavigableSet<DatoIntervallEntitet> utledPeriodeEtterHensynÅHaHensyntattFullstendigTidslinje(NavigableSet<DatoIntervallEntitet> perioder, NavigableSet<DatoIntervallEntitet> datoIntervallEntitets) {
        LocalDateTimeline<Boolean> perioderTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(perioder);
        var fullstendigTidslinje = opprettTidslinje(datoIntervallEntitets);

        var relevantTidslinje = new LocalDateTimeline<>(fullstendigTidslinje.stream()
            .filter(segment -> !perioderTidslinje.intersection(segment.getLocalDateInterval()).isEmpty())
            .toList());

        return TidslinjeUtil.tilDatoIntervallEntiteter(relevantTidslinje.compress());
    }

    private LocalDateTimeline<Boolean> opprettTidslinje(NavigableSet<DatoIntervallEntitet> datoIntervallEntitets) {
        List<LocalDateSegment<Boolean>> segmenter = datoIntervallEntitets.stream()
            .map(periode -> new LocalDateSegment<>(justerMotHelg(periode).toLocalDateInterval(), true))
            .toList();
        var tidslinje = new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceRightHandSide);
        return tidslinje.compress();
    }

    private DatoIntervallEntitet justerMotHelg(DatoIntervallEntitet it) {
        if (it.getTomDato().getDayOfWeek().equals(DayOfWeek.FRIDAY)) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(it.getFomDato(), it.getTomDato().plusDays(2));
        }
        if (it.getTomDato().getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(it.getFomDato(), it.getTomDato().plusDays(1));
        }
        return it;
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        final var vilkårPeriodeSet = new EnumMap<VilkårType, NavigableSet<DatoIntervallEntitet>>(VilkårType.class);
        UtledeteVilkår utledeteVilkår = vilkårUtleder.utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledPeriode(behandlingId, vilkår)));

        return vilkårPeriodeSet;
    }

    @WithSpan
    @Override
    public NavigableSet<DatoIntervallEntitet> utledFullstendigePerioder(@SpanAttribute("behandlingId") Long behandlingId) {
        return søknadsperiodeTjeneste.utledFullstendigPeriode(behandlingId);
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }

    private NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId, VilkårType vilkår) {
        return vilkårsPeriodisering.getOrDefault(vilkår, søktePerioder).utledPeriode(behandlingId);
    }

    @WithSpan
    @Override
    public NavigableSet<PeriodeMedÅrsak> utledRevurderingPerioder(BehandlingReferanse referanse) {
        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());
        var periodeMedÅrsaks = new TreeSet<PeriodeMedÅrsak>();
        if (skalVurdereBerørtePerioderPåBarnet(behandling)) {
            var berørtePerioderMedÅrsaker = finnBerørtePerioderPåPleietrengende(referanse);
            periodeMedÅrsaks.addAll(berørtePerioderMedÅrsaker);
            // TODO: Vurder om uttak skal være med inn her
        }
        periodeMedÅrsaks.addAll(revurderingPerioderTjeneste.utledPerioderFraProsessTriggereMedÅrsak(referanse));
        var utsattBehandlingAvPeriode = utsattBehandlingAvPeriodeRepository.hentGrunnlag(referanse.getBehandlingId());
        if (utsattBehandlingAvPeriode.isPresent()) {
            periodeMedÅrsaks.addAll(utsattBehandlingAvPeriode.stream()
                .map(UtsattBehandlingAvPeriode::getPerioder)
                .flatMap(Collection::stream)
                .map(it -> new PeriodeMedÅrsak(it.getPeriode(), BehandlingÅrsakType.RE_UTSATT_BEHANDLING))
                .toList());
        }

        periodeMedÅrsaks.addAll(revurderingPerioderTjeneste.utledPerioderFraInntektsmeldinger(referanse, finnKomprimertePerioderFraKantIKantVurderer(behandling))
            .stream()
            .map(it -> new PeriodeMedÅrsak(it, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING))
            .collect(Collectors.toSet()));

        return periodeMedÅrsaks;
    }

    private NavigableSet<DatoIntervallEntitet> finnKomprimertePerioderFraKantIKantVurderer(Behandling behandling) {
        var fullstendigePerioder = utledFullstendigePerioder(behandling.getId());
        var periodetidslinje = TidslinjeUtil.tilTidslinjeKomprimert(fullstendigePerioder);
        var sammenhengendeTidslinje = periodetidslinje.compress((d1, d2) -> getKantIKantVurderer().erKantIKant(DatoIntervallEntitet.fra(d1), DatoIntervallEntitet.fra(d2)), Boolean::equals, StandardCombinators::alwaysTrueForMatch);
        return sammenhengendeTidslinje.getLocalDateIntervals().stream().map(DatoIntervallEntitet::fra).collect(Collectors.toCollection(TreeSet::new));
    }

    private NavigableSet<PeriodeMedÅrsak> finnBerørtePerioderPåPleietrengende(BehandlingReferanse referanse) {
        var tidslinjeMedÅrsaker = pleitrengendeRevurderingPerioderTjeneste.utledBerørtePerioderPåPleietrengende(referanse, definerendeVilkår());
        var unikeÅrsaker = tidslinjeMedÅrsaker.stream().flatMap(s -> s.getValue().stream()).collect(Collectors.toSet());
        return unikeÅrsaker.stream().flatMap(årsak -> tidslinjeMedÅrsaker.filterValue(s -> s.contains(årsak))
                .compress()
                .getLocalDateIntervals()
                .stream().map(p -> new PeriodeMedÅrsak(DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato()), årsak)))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean skalVurdereBerørtePerioderPåBarnet(Behandling behandling) {
        return behandling.getOriginalBehandlingId().isPresent();
    }

    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return erKantIKantVurderer;
    }

    @WithSpan
    @Override
    public NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles(Long behandlingId) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        return søknadsperiodeTjeneste.hentKravperioder(BehandlingReferanse.fra(behandling))
            .stream()
            .filter(kp -> kp.isHarTrukketKrav() && kp.getBehandlingId().equals(behandlingId))
            .map(SøknadsperiodeTjeneste.Kravperiode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private LocalDateTimeline<Boolean> uttaksendringerSidenForrigeBehandling(BehandlingReferanse referanse) {
        var segments = endringIUttakPeriodeUtleder.utled(referanse).stream()
            .map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), Boolean.TRUE))
            .toList();
        return new LocalDateTimeline<>(segments);
    }

}
