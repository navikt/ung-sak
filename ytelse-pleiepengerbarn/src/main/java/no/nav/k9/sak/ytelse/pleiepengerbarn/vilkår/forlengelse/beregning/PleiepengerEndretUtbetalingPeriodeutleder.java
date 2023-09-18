package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static java.lang.Boolean.TRUE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.domene.opptjening.MellomliggendeHelgUtleder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.EndretUtbetalingPeriodeutleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@ApplicationScoped
public class PleiepengerEndretUtbetalingPeriodeutleder implements EndretUtbetalingPeriodeutleder {

    private UttakTjeneste uttakRestKlient;
    private BehandlingRepository behandlingRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private ProsessTriggereRepository prosessTriggereRepository;

    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private boolean enabled;


    public PleiepengerEndretUtbetalingPeriodeutleder() {
    }

    @Inject
    public PleiepengerEndretUtbetalingPeriodeutleder(UttakTjeneste uttakRestKlient,
                                                     BehandlingRepository behandlingRepository,
                                                     @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                                     ProsessTriggereRepository prosessTriggereRepository,
                                                     SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                     @KonfigVerdi(value = "BG_FORLENGELSE_BASERT_PAA_UTTAK", defaultVerdi = "true") boolean enabled) {
        this.uttakRestKlient = uttakRestKlient;
        this.behandlingRepository = behandlingRepository;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.enabled = enabled;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse) {
        var periodeTjeneste = getPeriodeTjeneste(behandlingReferanse);
        return periodeTjeneste.utled(behandlingReferanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR).stream()
            .flatMap(p -> utledPerioder(behandlingReferanse, p).stream())
            .collect(Collectors.toCollection(TreeSet::new));

    }

    private VilkårsPerioderTilVurderingTjeneste getPeriodeTjeneste(BehandlingReferanse behandlingReferanse) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsperiode) {

        if (!enabled) {
            return new TreeSet<>(Set.of(vilkårsperiode));
        }

        var tidslinjeFraProessTriggere = finnTidslinjeFraProsessTriggere(behandlingReferanse);
        var søknadperioderForBehandlingTidslinje = finnTidslinjeForRelevanteSøknadsperioder(behandlingReferanse);
        var påvirketAvUttaksendringTidslinje = finnTidslinjePåvirketAvUttaksendring(behandlingReferanse, vilkårsperiode);

        var tidslinje = påvirketAvUttaksendringTidslinje
            .crossJoin(søknadperioderForBehandlingTidslinje, StandardCombinators::coalesceLeftHandSide)
            .crossJoin(tidslinjeFraProessTriggere, StandardCombinators::coalesceLeftHandSide)
            .compress();

        tidslinje = fyllMellomromDersomKunHelg(tidslinje).compress();

        return finnUttaksendringerSomOverlapperEllerErKantiKantMedPerioden(vilkårsperiode, tidslinje);
    }

    private static LocalDateTimeline<Boolean> fyllMellomromDersomKunHelg(LocalDateTimeline<Boolean> tidslinje) {
        var mellomliggendeHelgUtleder = new MellomliggendeHelgUtleder();
        var mellomliggendePerioder = mellomliggendeHelgUtleder.beregnMellomliggendeHelg(tidslinje);
        tidslinje = tidslinje.combine(mellomliggendePerioder, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        return tidslinje;
    }

    private LocalDateTimeline<Boolean> finnTidslinjeFraProsessTriggere(BehandlingReferanse behandlingReferanse) {
        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var perioderFraTriggere = prosessTriggere.stream().flatMap(it -> it.getTriggere().stream())
            .filter(it -> it.getÅrsak().equals(BehandlingÅrsakType.RE_ENDRET_FORDELING))
            .map(Trigger::getPeriode)
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), TRUE))
            .collect(Collectors.toSet());
        return new LocalDateTimeline<>(perioderFraTriggere);
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForRelevanteSøknadsperioder(BehandlingReferanse behandlingReferanse) {
        var relevantePerioder = søknadsperiodeTjeneste.utledPeriode(behandlingReferanse.getBehandlingId(), false);
        var søknadsperioderForBehandling = relevantePerioder
            .stream()
            .map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), TRUE)).toList();
        return new LocalDateTimeline<>(søknadsperioderForBehandling, StandardCombinators::coalesceLeftHandSide);
    }

    private LocalDateTimeline<Boolean> finnTidslinjePåvirketAvUttaksendring(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode) {
        var originalBehandlingId = behandlingReferanse.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Forventer å finne original behandling"));
        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);

        var vilkårFraOriginalBehandling = getPeriodeTjeneste(behandlingReferanse)
            .utled(originalBehandlingId, VilkårType.BEREGNINGSGRUNNLAGVILKÅR).stream().map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toSet());

        var uttaksplan = uttakRestKlient.hentUttaksplan(behandlingReferanse.getBehandlingUuid(), true);
        var originalUttakslpan = uttakRestKlient.hentUttaksplan(originalBehandling.getUuid(), true);

        var uttakTidslinje = lagTidslinje(uttaksplan);
        var originalUttakTidslinje = lagTidslinje(originalUttakslpan);

        var uttaksendringer = uttakTidslinje.combine(originalUttakTidslinje, TidslinjeUtil::forskjell, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        var resultatperioder = new ArrayList<DatoIntervallEntitet>();
        resultatperioder.addAll(finnUttaksendringerSomOverlapperEllerErKantiKantMedPerioden(periode, uttaksendringer));
        resultatperioder.addAll(finnVilkårsperioderSomUmiddelbartEtterfølgerUttaksendringer(uttaksendringer, vilkårFraOriginalBehandling));

        return new LocalDateTimeline<>(resultatperioder.stream().map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), TRUE)).toList(), StandardCombinators::alwaysTrueForMatch);
    }

    private List<DatoIntervallEntitet> finnVilkårsperioderSomUmiddelbartEtterfølgerUttaksendringer(LocalDateTimeline<?> uttaksendringer, Set<DatoIntervallEntitet> vilkårFraOrginalBehandling) {
        var kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();
        return vilkårFraOrginalBehandling.stream()
            .filter(gammelVilkårsperiode -> uttaksendringer.stream()
                .anyMatch(uttaksendring -> uttaksendring.getTom().isBefore(gammelVilkårsperiode.getTomDato()) && kantIKantVurderer.erKantIKant(gammelVilkårsperiode, tilIntervall(uttaksendring))))
            .toList();
    }

    private DatoIntervallEntitet tilIntervall(LocalDateSegment<?> s) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom());
    }

    private <V> NavigableSet<DatoIntervallEntitet> finnUttaksendringerSomOverlapperEllerErKantiKantMedPerioden(DatoIntervallEntitet vilkårsperiode, LocalDateTimeline<V> differanse) {
        var kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();

        var intervaller = differanse.toSegments().stream()
            .map(this::tilIntervall)
            .sorted(Comparator.naturalOrder())
            .toList();

        var resultat = new TreeSet<DatoIntervallEntitet>();

        for (var intervall : intervaller) {
            if (intervall.overlapper(vilkårsperiode)) {
                resultat.add(intervall);
            } else if (resultat.stream().anyMatch(r -> kantIKantVurderer.erKantIKant(intervall, r)) || kantIKantVurderer.erKantIKant(intervall, vilkårsperiode)) {
                resultat.add(intervall);
            }
        }


        return resultat;
    }

    private LocalDateTimeline<Set<Utbetalingsgrader>> lagTidslinje(Uttaksplan uttaksplan) {
        Set<LocalDateSegment<Set<Utbetalingsgrader>>> segmenter = uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .map(e -> new LocalDateSegment<>(e.getKey().getFom(), e.getKey().getTom(),
                e.getValue().getUtbetalingsgrader().stream().collect(Collectors.toSet())))
            .collect(Collectors.toSet());

        return new LocalDateTimeline<>(segmenter);
    }


}
