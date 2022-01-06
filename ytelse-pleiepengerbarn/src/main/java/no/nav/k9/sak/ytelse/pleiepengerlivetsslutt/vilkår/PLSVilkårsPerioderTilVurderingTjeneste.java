package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.vilkår;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.PeriodeMedÅrsak;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.MaksSøktePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SøktePerioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.RevurderingPerioderTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Endringsstatus;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@FagsakYtelseTypeRef("PPN")
@BehandlingTypeRef
@ApplicationScoped
public class PLSVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    // TODO PLS: Samle felles logikk med PSBVilkårsPerioderTilVurderingTjeneste, eller forenkle denne radikalt

    private final PåTversAvHelgErKantIKantVurderer erKantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();

    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering = new HashMap<>();
    private VilkårUtleder vilkårUtleder;
    private SøktePerioder søktePerioder;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;

    private RevurderingPerioderTjeneste revurderingPerioderTjeneste;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private UttakTjeneste uttakTjeneste;

    PLSVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public PLSVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef("PPN") VilkårUtleder vilkårUtleder,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  SykdomGrunnlagService sykdomGrunnlagService,
                                                  RevurderingPerioderTjeneste revurderingPerioderTjeneste,
                                                  SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                  UttakTjeneste uttakTjeneste) {
        this.vilkårUtleder = vilkårUtleder;
        this.behandlingRepository = behandlingRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.revurderingPerioderTjeneste = revurderingPerioderTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.uttakTjeneste = uttakTjeneste;

        søktePerioder = new SøktePerioder(søknadsperiodeTjeneste);
        var maksSøktePeriode = new MaksSøktePeriode(søknadsperiodeTjeneste);

        vilkårsPeriodisering.put(VilkårType.MEDLEMSKAPSVILKÅRET, maksSøktePeriode);
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var perioder = utledPeriode(behandlingId, vilkårType);
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        if (vilkårene.isPresent()) {
            return utledVilkårsPerioderFraPerioderTilVurdering(behandlingId, vilkårene.get(), perioder);
        }
        return utledPeriode(behandlingId, vilkårType);
    }

    private NavigableSet<DatoIntervallEntitet> utledVilkårsPerioderFraPerioderTilVurdering(Long behandlingId, Vilkår vilkår, Set<DatoIntervallEntitet> perioder) {
        var perioderTilVurdering = new TreeSet<>(utledPerioderTilVurderingVedÅHensyntaFullstendigTidslinje(behandlingId, perioder));
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var referanse = BehandlingReferanse.fra(behandling);
        if (skalVurdereBerørtePerioderPåPleietrengende(behandling)) {
            var berørtePerioder = utledUtvidetPeriode(referanse);
            perioderTilVurdering.addAll(berørtePerioder);
        }

        perioderTilVurdering.addAll(revurderingPerioderTjeneste.utledPerioderFraProsessTriggere(referanse));
        perioderTilVurdering.addAll(revurderingPerioderTjeneste.utledPerioderFraInntektsmeldinger(referanse));
        perioderTilVurdering.addAll(perioderSomSkalTilbakestilles(behandlingId));

        return vilkår.getPerioder()
            .stream()
            .map(VilkårPeriode::getPeriode)
            .filter(datoIntervallEntitet -> perioderTilVurdering.stream().anyMatch(it -> datoIntervallEntitet.overlapper(it.getFomDato().minusDays(1), it.getTomDato().plusDays(1))))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<DatoIntervallEntitet> utledPerioderTilVurderingVedÅHensyntaFullstendigTidslinje(Long behandlingId, Set<DatoIntervallEntitet> perioder) {
        var datoIntervallEntitets = utledFullstendigePerioder(behandlingId);
        return utledPeriodeEtterHensynÅHaHensyntattFullstendigTidslinje(perioder, datoIntervallEntitets);
    }

    NavigableSet<DatoIntervallEntitet> utledPeriodeEtterHensynÅHaHensyntattFullstendigTidslinje(Set<DatoIntervallEntitet> perioder, NavigableSet<DatoIntervallEntitet> datoIntervallEntitets) {
        var fullstendigTidslinje = opprettTidslinje(datoIntervallEntitets)
            .toSegments()
            .stream().map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            .collect(Collectors.toCollection(TreeSet::new));

        var relevantTidslinje = new LocalDateTimeline<>(
            fullstendigTidslinje.stream()
                .filter(it -> perioder.stream().anyMatch(it::overlapper))
                .map(DatoIntervallEntitet::toLocalDateInterval)
                .map(it -> new LocalDateSegment<>(it, true))
                .collect(Collectors.toList()))
            .compress();

        return relevantTidslinje.toSegments()
            .stream()
            .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private LocalDateTimeline<Boolean> opprettTidslinje(NavigableSet<DatoIntervallEntitet> datoIntervallEntitets) {

        var tidslinje = new LocalDateTimeline<Boolean>(List.of());

        for (DatoIntervallEntitet periode : datoIntervallEntitets) {
            var segmentLinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(justerMotHelg(periode).toLocalDateInterval(), true)));
            tidslinje = tidslinje.combine(segmentLinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

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

    @Override
    public NavigableSet<DatoIntervallEntitet> utledUtvidetRevurderingPerioder(BehandlingReferanse referanse) {
        final var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingUuid());

        final var perioder = utled(referanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        final var vurderingsperioderTimeline = SykdomUtils.toLocalDateTimeline(perioder);
        List<Periode> nyeVurderingsperioder = SykdomUtils.toPeriodeList(perioder);

        final LocalDateTimeline<Boolean> endringerISøktePerioder = sykdomGrunnlagService.utledRelevanteEndringerSidenForrigeBehandling(behandling, nyeVurderingsperioder)
            .getDiffPerioder();

        final LocalDateTimeline<Boolean> utvidedePerioder = SykdomUtils.kunPerioderSomIkkeFinnesI(endringerISøktePerioder, vurderingsperioderTimeline);

        var ekstraPerioder = utvidedePerioder.stream()
            .map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom())).collect(Collectors.toCollection(TreeSet::new));

        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(referanse.getBehandlingId())
            .flatMap(it -> it.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR));
        if (vilkårene.isPresent()) {
            return utledVilkårsPerioderFraPerioderTilVurdering(referanse.getBehandlingId(), vilkårene.get(), ekstraPerioder);
        }
        return ekstraPerioder;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledFullstendigePerioder(Long behandlingId) {
        return søknadsperiodeTjeneste.utledFullstendigPeriode(behandlingId);
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }

    private NavigableSet<DatoIntervallEntitet> utledPeriode(Long behandlingId, VilkårType vilkår) {
        return vilkårsPeriodisering.getOrDefault(vilkår, søktePerioder).utledPeriode(behandlingId);
    }

    @Override
    public NavigableSet<PeriodeMedÅrsak> utledRevurderingPerioder(BehandlingReferanse referanse) {
        var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingId());
        var periodeMedÅrsaks = new TreeSet<PeriodeMedÅrsak>();
        if (skalVurdereBerørtePerioderPåPleietrengende(behandling)) {
            periodeMedÅrsaks.addAll(utledUtvidetPeriode(referanse)
                .stream()
                .map(it -> new PeriodeMedÅrsak(it, BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON))
                .collect(Collectors.toSet()));
        }
        periodeMedÅrsaks.addAll(revurderingPerioderTjeneste.utledPerioderFraProsessTriggereMedÅrsak(referanse));
        periodeMedÅrsaks.addAll(revurderingPerioderTjeneste.utledPerioderFraInntektsmeldinger(referanse)
            .stream()
            .map(it -> new PeriodeMedÅrsak(it, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING))
            .collect(Collectors.toSet()));

        return periodeMedÅrsaks;
    }

    private NavigableSet<DatoIntervallEntitet> utledUtvidetPeriode(BehandlingReferanse referanse) {
        LocalDateTimeline<Boolean> utvidedePerioder = utledUtvidetPeriodeForSykdom(referanse);
        utvidedePerioder = utvidedePerioder.union(uttaksendringerSidenForrigeBehandling(referanse), StandardCombinators::alwaysTrueForMatch);

        return utvidedePerioder.toSegments()
            .stream()
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    @SuppressWarnings("unchecked")
    private LocalDateTimeline<Boolean> uttaksendringerSidenForrigeBehandling(BehandlingReferanse referanse) {
        final Uttaksplan uttaksplan = uttakTjeneste.hentUttaksplan(referanse.getBehandlingUuid(), false);
        if (uttaksplan == null) {
            return LocalDateTimeline.EMPTY_TIMELINE;
        }

        final List<LocalDateSegment<Boolean>> segments = uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getEndringsstatus() != Endringsstatus.UENDRET)
            .map(entry -> new LocalDateSegment<Boolean>(entry.getKey().getFom(), entry.getKey().getTom(), Boolean.TRUE))
            .toList();

        return new LocalDateTimeline<Boolean>(segments);
    }

    private LocalDateTimeline<Boolean> utledUtvidetPeriodeForSykdom(BehandlingReferanse referanse) {
        var forrigeVedtatteBehandling = behandlingRepository.hentBehandling(referanse.getOriginalBehandlingId().orElseThrow()).getUuid();
        var vedtattSykdomGrunnlagBehandling = sykdomGrunnlagService.hentGrunnlag(forrigeVedtatteBehandling);
        var pleietrengende = referanse.getPleietrengendeAktørId();
        var vilkårene = vilkårResultatRepository.hent(referanse.getId());
        var vurderingsperioder = utledVurderingsperiode(vilkårene);

        var utledetGrunnlag = sykdomGrunnlagService.utledGrunnlagMedManglendeOmsorgFjernet(referanse.getSaksnummer(), referanse.getBehandlingUuid(), referanse.getBehandlingId(), pleietrengende, vurderingsperioder);

        final LocalDateTimeline<Boolean> endringerISøktePerioder = sykdomGrunnlagService.sammenlignGrunnlag(Optional.of(vedtattSykdomGrunnlagBehandling.getGrunnlag()), utledetGrunnlag).getDiffPerioder();
        return endringerISøktePerioder;
    }

    private boolean skalVurdereBerørtePerioderPåPleietrengende(Behandling behandling) {
        return behandling.getOriginalBehandlingId().isPresent();
    }

    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return erKantIKantVurderer;
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        return Set.of(VilkårType.I_LIVETS_SLUTTFASE);
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles(Long behandlingId) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        return søknadsperiodeTjeneste.hentKravperioder(behandling.getFagsakId(), behandlingId)
            .stream()
            .filter(kp -> kp.isHarTrukketKrav() && kp.getBehandlingId().equals(behandlingId))
            .map(SøknadsperiodeTjeneste.Kravperiode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<Periode> utledVurderingsperiode(Vilkårene vilkårene) {
        return vilkårene.getVilkår(VilkårType.I_LIVETS_SLUTTFASE)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
