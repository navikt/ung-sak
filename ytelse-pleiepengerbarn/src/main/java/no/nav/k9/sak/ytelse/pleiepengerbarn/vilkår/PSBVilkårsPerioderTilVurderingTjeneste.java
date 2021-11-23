package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;

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
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.perioder.PeriodeMedÅrsak;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VilkårsPeriodiseringsFunksjon;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.ErEndringPåEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomUtils;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.EndringUnntakEtablertTilsynTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.RevurderingPerioderTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Endringsstatus;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

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
    private SykdomGrunnlagService sykdomGrunnlagService;
    private ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste;

    private EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste;
    private RevurderingPerioderTjeneste revurderingPerioderTjeneste;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private UttakTjeneste uttakTjeneste;

    PSBVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public PSBVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef("PSB") VilkårUtleder vilkårUtleder,
                                                  SøknadsperiodeRepository søknadsperiodeRepository,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  SykdomGrunnlagService sykdomGrunnlagService,
                                                  ErEndringPåEtablertTilsynTjeneste etablertTilsynTjeneste,
                                                  EndringUnntakEtablertTilsynTjeneste endringUnntakEtablertTilsynTjeneste,
                                                  BasisPersonopplysningTjeneste basisPersonopplysningsTjeneste,
                                                  RevurderingPerioderTjeneste revurderingPerioderTjeneste,
                                                  PersoninfoAdapter personinfoAdapter,
                                                  MottatteDokumentRepository mottatteDokumentRepository,
                                                  SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                  UttakTjeneste uttakTjeneste,
                                                  @KonfigVerdi(value = "ENABLE_RELEVANT_SYKDOMSPERIODE", defaultVerdi = "false") boolean brukRelevantPeriode) {
        this.vilkårUtleder = vilkårUtleder;
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.behandlingRepository = behandlingRepository;
        this.sykdomGrunnlagService = sykdomGrunnlagService;
        this.etablertTilsynTjeneste = etablertTilsynTjeneste;
        this.endringUnntakEtablertTilsynTjeneste = endringUnntakEtablertTilsynTjeneste;
        this.revurderingPerioderTjeneste = revurderingPerioderTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.uttakTjeneste = uttakTjeneste;

        søktePerioder = new SøktePerioder(søknadsperiodeTjeneste);
        var maksSøktePeriode = new MaksSøktePeriode(søknadsperiodeTjeneste);

        vilkårsPeriodisering.put(VilkårType.MEDLEMSKAPSVILKÅRET, maksSøktePeriode);
        vilkårsPeriodisering.put(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, PleietrengendeAlderPeriode.under18(basisPersonopplysningsTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste, brukRelevantPeriode));
        vilkårsPeriodisering.put(VilkårType.MEDISINSKEVILKÅR_18_ÅR, PleietrengendeAlderPeriode.overEllerLik18(basisPersonopplysningsTjeneste, behandlingRepository, personinfoAdapter, søknadsperiodeTjeneste, brukRelevantPeriode));
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
        if (skalVurdereBerørtePerioderPåBarnet(behandling)) {
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
        var fullstendigTidslinje = new LocalDateTimeline<>(utledFullstendigePerioder(behandlingId)
            .stream()
            .map(DatoIntervallEntitet::toLocalDateInterval)
            .map(it -> new LocalDateSegment<>(it, true))
            .collect(Collectors.toList()));
        var relevantTidslinje = new LocalDateTimeline<>(perioder.stream()
            .map(DatoIntervallEntitet::toLocalDateInterval)
            .map(it -> new LocalDateSegment<>(it, true))
            .collect(Collectors.toList()));

        return fullstendigTidslinje.intersection(relevantTidslinje).compress()
            .stream()
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()))
            .collect(Collectors.toCollection(TreeSet::new));
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
        if (skalVurdereBerørtePerioderPåBarnet(behandling)) {
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
        utvidedePerioder = utvidedePerioder.union(etablertTilsynTjeneste.perioderMedEndringerFraForrigeBehandling(referanse), StandardCombinators::alwaysTrueForMatch);
        utvidedePerioder = utvidedePerioder.union(endringUnntakEtablertTilsynTjeneste.perioderMedEndringerSidenBehandling(referanse.getOriginalBehandlingId().orElse(null), referanse.getPleietrengendeAktørId()), StandardCombinators::alwaysTrueForMatch);
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

    private boolean skalVurdereBerørtePerioderPåBarnet(Behandling behandling) {
        return behandling.getOriginalBehandlingId().isPresent();
    }

    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return erKantIKantVurderer;
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        return Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR);
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
        var vurderingsperioder = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toCollection(ArrayList::new));

        vurderingsperioder.addAll(vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR)
            .map(Vilkår::getPerioder)
            .orElse(List.of())
            .stream()
            .map(VilkårPeriode::getPeriode)
            .map(it -> new Periode(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toList()));

        return vurderingsperioder;
    }
}
