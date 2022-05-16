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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.PeriodeMedÅrsak;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriode;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.RevurderingPerioderTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Endringsstatus;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;


public abstract class PleiepengerVilkårsPerioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private final PåTversAvHelgErKantIKantVurderer erKantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();

    private Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering;
    private VilkårUtleder vilkårUtleder;
    private SøktePerioder søktePerioder;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private SykdomGrunnlagService sykdomGrunnlagService;
    private ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste;

    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;
    private RevurderingPerioderTjeneste revurderingPerioderTjeneste;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository;
    private UttakTjeneste uttakTjeneste;

    public PleiepengerVilkårsPerioderTilVurderingTjeneste() {
    }

    @Inject
    public PleiepengerVilkårsPerioderTilVurderingTjeneste(VilkårUtleder vilkårUtleder,
                                                          Map<VilkårType, VilkårsPeriodiseringsFunksjon> vilkårsPeriodisering,
                                                          VilkårResultatRepository vilkårResultatRepository,
                                                          BehandlingRepository behandlingRepository,
                                                          SykdomGrunnlagService sykdomGrunnlagService,
                                                          ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste,
                                                          EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                                          RevurderingPerioderTjeneste revurderingPerioderTjeneste,
                                                          SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                          UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                                          UttakTjeneste uttakTjeneste) {
        this.vilkårUtleder = vilkårUtleder;
        this.vilkårsPeriodisering = vilkårsPeriodisering;
        this.behandlingRepository = behandlingRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
        this.revurderingPerioderTjeneste = revurderingPerioderTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.utsattBehandlingAvPeriodeRepository = utsattBehandlingAvPeriodeRepository;
        this.uttakTjeneste = uttakTjeneste;

        søktePerioder = new SøktePerioder(søknadsperiodeTjeneste);
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

    private NavigableSet<DatoIntervallEntitet> utledVilkårsPerioderFraPerioderTilVurdering(Long behandlingId, Vilkår vilkår, NavigableSet<DatoIntervallEntitet> perioder) {
        var perioderTilVurdering = new TreeSet<>(utledPerioderTilVurderingVedÅHensyntaFullstendigTidslinje(behandlingId, perioder));
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var referanse = BehandlingReferanse.fra(behandling);
        if (skalVurdereBerørtePerioderPåBarnet(behandling)) {
            var berørtePerioder = utledUtvidetPeriode(referanse);
            perioderTilVurdering.addAll(berørtePerioder);
        }

        perioderTilVurdering.addAll(revurderingPerioderTjeneste.utledPerioderFraProsessTriggere(referanse));
        perioderTilVurdering.addAll(revurderingPerioderTjeneste.utledPerioderFraInntektsmeldinger(referanse, utledFullstendigePerioder(behandling.getId())));
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
        LocalDateTimeline<Boolean> perioderTidslinje = SykdomUtils.toLocalDateTimeline(perioder);
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

    @Override
    public NavigableSet<DatoIntervallEntitet> utledUtvidetRevurderingPerioder(BehandlingReferanse referanse) {
        final var behandling = behandlingRepository.hentBehandling(referanse.getBehandlingUuid());

        final var perioder = utled(referanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        final var vurderingsperioderTimeline = SykdomUtils.toLocalDateTimeline(perioder);
        List<Periode> nyeVurderingsperioder = SykdomUtils.toPeriodeList(perioder);

        final LocalDateTimeline<Boolean> endringerISøktePerioder = sykdomGrunnlagService.utledRelevanteEndringerSidenForrigeBehandling(behandling, nyeVurderingsperioder)
            .getDiffPerioder();

        final LocalDateTimeline<Boolean> utvidedePerioder = SykdomUtils.kunPerioderSomIkkeFinnesI(endringerISøktePerioder, vurderingsperioderTimeline);

        var ekstraPerioder = TidslinjeUtil.tilDatoIntervallEntiteter(utvidedePerioder);

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
        if (skalVurdereBerørtePerioderPåBarnet(behandling)) {
            periodeMedÅrsaks.addAll(utledUtvidetPeriodeForSykdom(referanse)
                .toSegments()
                .stream()
                .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
                .map(it -> new PeriodeMedÅrsak(it, BehandlingÅrsakType.RE_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON))
                .collect(Collectors.toSet()));
            periodeMedÅrsaks.addAll(etablertTilsynTjeneste.perioderMedEndringerFraForrigeBehandling(referanse)
                .toSegments()
                .stream()
                .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
                .map(it -> new PeriodeMedÅrsak(it, BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON))
                .collect(Collectors.toSet()));
            periodeMedÅrsaks.addAll(endringUnntakEtablertTilsynTjeneste.perioderMedEndringerSidenBehandling(referanse.getOriginalBehandlingId().orElse(null), referanse.getPleietrengendeAktørId())
                .toSegments()
                .stream()
                .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
                .map(it -> new PeriodeMedÅrsak(it, BehandlingÅrsakType.RE_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON))
                .collect(Collectors.toSet()));
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
        periodeMedÅrsaks.addAll(revurderingPerioderTjeneste.utledPerioderFraInntektsmeldinger(referanse, utledFullstendigePerioder(behandling.getId()))
            .stream()
            .map(it -> new PeriodeMedÅrsak(it, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING))
            .collect(Collectors.toSet()));

        return periodeMedÅrsaks;
    }

    private NavigableSet<DatoIntervallEntitet> utledUtvidetPeriode(BehandlingReferanse referanse) {
        LocalDateTimeline<Boolean> utvidedePerioder = utledUtvidetPeriodeForSykdom(referanse);
        utvidedePerioder = utvidedePerioder.union(etablertTilsynTjeneste.perioderMedEndringerFraForrigeBehandling(referanse), StandardCombinators::alwaysTrueForMatch);
        utvidedePerioder = utvidedePerioder.union(endringUnntakEtablertTilsynTjeneste.perioderMedEndringerSidenBehandling(referanse.getOriginalBehandlingId().orElse(null), referanse.getPleietrengendeAktørId()), StandardCombinators::alwaysTrueForMatch);
        utvidedePerioder = utvidedePerioder.union(uttaksendringerSidenForrigeBehandling(referanse), StandardCombinators::alwaysTrueForMatch);

        return TidslinjeUtil.tilDatoIntervallEntiteter(utvidedePerioder);
    }

    private LocalDateTimeline<Boolean> uttaksendringerSidenForrigeBehandling(BehandlingReferanse referanse) {
        final Uttaksplan uttaksplan = uttakTjeneste.hentUttaksplan(referanse.getBehandlingUuid(), false);
        if (uttaksplan == null) {
            return LocalDateTimeline.empty();
        }

        final List<LocalDateSegment<Boolean>> segments = uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getEndringsstatus() != Endringsstatus.UENDRET)
            .map(entry -> new LocalDateSegment<Boolean>(entry.getKey().getFom(), entry.getKey().getTom(), Boolean.TRUE))
            .toList();

        return new LocalDateTimeline<>(segments);
    }

    private LocalDateTimeline<Boolean> utledUtvidetPeriodeForSykdom(BehandlingReferanse referanse) {
        var forrigeVedtatteBehandling = behandlingRepository.hentBehandling(referanse.getOriginalBehandlingId().orElseThrow()).getUuid();
        var vedtattSykdomGrunnlagBehandling = sykdomGrunnlagService.hentGrunnlagHvisEksisterer(forrigeVedtatteBehandling);
        var pleietrengende = referanse.getPleietrengendeAktørId();
        var vilkårene = vilkårResultatRepository.hent(referanse.getId());
        var vurderingsperioder = utledVurderingsperiode(vilkårene);

        var utledetGrunnlag = sykdomGrunnlagService.utledGrunnlagMedManglendeOmsorgFjernet(referanse.getSaksnummer(), referanse.getBehandlingUuid(), referanse.getBehandlingId(), pleietrengende, vurderingsperioder);

        return sykdomGrunnlagService.sammenlignGrunnlag(vedtattSykdomGrunnlagBehandling.map(SykdomGrunnlagBehandling::getGrunnlag), utledetGrunnlag).getDiffPerioder();
    }

    private boolean skalVurdereBerørtePerioderPåBarnet(Behandling behandling) {
        return behandling.getOriginalBehandlingId().isPresent();
    }

    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return erKantIKantVurderer;
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
        return definerendeVilkår().stream()
            .flatMap(vt -> vilkårene.getVilkår(vt).stream())
            .flatMap(v -> v.getPerioder().stream())
            .map(VilkårPeriode::getPeriode)
            .map(p -> new Periode(p.getFomDato(), p.getTomDato()))
            .toList();
    }
}
