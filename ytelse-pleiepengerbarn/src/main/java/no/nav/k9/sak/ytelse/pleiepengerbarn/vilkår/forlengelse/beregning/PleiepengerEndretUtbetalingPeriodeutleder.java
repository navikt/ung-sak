package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static java.lang.Boolean.TRUE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.ENDRING_I_DATO_NYE_UTTAK_REGLER;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.ENDRING_I_PERSONOPPLYSNING_PLEIETRENGENDE;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.ENDRING_I_PERSONOPPLYSNING_SØKER;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.ENDRING_I_REFUSJONSKRAV;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.SØKNAD_FRA_BRUKER;

import java.time.LocalDate;
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
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
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
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.ErEndringIRefusjonskravVurderer;
import no.nav.k9.sak.domene.opptjening.MellomliggendeHelgUtleder;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.EndretUtbetalingPeriodeutleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.PleietrengendeRevurderingPerioderTjeneste;
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
    private UttakNyeReglerRepository uttakNyeReglerRepository;
    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private PleietrengendeRevurderingPerioderTjeneste pleietrengendeRevurderingPerioderTjeneste;
    private ErEndringIRefusjonskravVurderer erEndringIRefusjonskravVurderer;
    private boolean utvidetUtlederEnabled;

    public PleiepengerEndretUtbetalingPeriodeutleder() {
    }

    @Inject
    public PleiepengerEndretUtbetalingPeriodeutleder(UttakTjeneste uttakRestKlient,
                                                     BehandlingRepository behandlingRepository,
                                                     @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                                     ProsessTriggereRepository prosessTriggereRepository,
                                                     SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                     UttakNyeReglerRepository uttakNyeReglerRepository,
                                                     MapInputTilUttakTjeneste mapInputTilUttakTjeneste,
                                                     PersonopplysningTjeneste personopplysningTjeneste,
                                                     PleietrengendeRevurderingPerioderTjeneste pleietrengendeRevurderingPerioderTjeneste, ErEndringIRefusjonskravVurderer erEndringIRefusjonskravVurderer,
                                                     @KonfigVerdi(value = "UTVIDET_ENDRING_UTBETALING_UTLEDER", defaultVerdi = "false") boolean utvidetUtlederEnabled) {
        this.uttakRestKlient = uttakRestKlient;
        this.behandlingRepository = behandlingRepository;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.uttakNyeReglerRepository = uttakNyeReglerRepository;
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.pleietrengendeRevurderingPerioderTjeneste = pleietrengendeRevurderingPerioderTjeneste;
        this.erEndringIRefusjonskravVurderer = erEndringIRefusjonskravVurderer;
        this.utvidetUtlederEnabled = utvidetUtlederEnabled;
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
    public NavigableSet<DatoIntervallEntitet> utledPerioder(BehandlingReferanse behandlingReferanse,
                                                            DatoIntervallEntitet vilkårsperiode) {

        if (!utvidetUtlederEnabled) {

            var tidslinjeFraProessTriggere = finnTidslinjeFraProsessTriggere(behandlingReferanse);
            var søknadperioderForBehandlingTidslinje = finnTidslinjeForRelevanteSøknadsperioder(behandlingReferanse);
            var påvirketAvUttaksendringTidslinje = finnTidslinjePåvirketAvUttaksendring(behandlingReferanse, vilkårsperiode);
            var datoNyeReglerTidslinje = finnDatoNyeReglerTidslinje(behandlingReferanse, vilkårsperiode);
            var endringstidslinjeRefusjonskrav = erEndringIRefusjonskravVurderer.finnEndringstidslinjeForRefusjon(behandlingReferanse, vilkårsperiode);
            var tidslinje = påvirketAvUttaksendringTidslinje
                .crossJoin(endringstidslinjeRefusjonskrav)
                .crossJoin(søknadperioderForBehandlingTidslinje, StandardCombinators::coalesceLeftHandSide)
                .crossJoin(tidslinjeFraProessTriggere, StandardCombinators::coalesceLeftHandSide)
                .crossJoin(datoNyeReglerTidslinje, StandardCombinators::coalesceLeftHandSide)
                .compress();
            tidslinje = fyllMellomromDersomKunHelg(tidslinje).compress();

            return finnUttaksendringerSomOverlapperEllerErKantiKantMedPerioden(vilkårsperiode, tidslinje);
        } else {
            var tidslinje = finnÅrsakstidslinje(behandlingReferanse, vilkårsperiode);
            return finnPerioderRelevantForAktuellVilkårsperiode(behandlingReferanse, vilkårsperiode, tidslinje);
        }


    }

    public LocalDateTimeline<Set<EndringsårsakUtbetaling>> finnÅrsakstidslinje(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsperiode) {
        var utvidetRevurderingPerioder = finnBerørtePerioderPåBarnet(behandlingReferanse, vilkårsperiode);
        var endringstidslinjeRefusjonskrav = erEndringIRefusjonskravVurderer.finnEndringstidslinjeForRefusjon(behandlingReferanse, vilkårsperiode).mapValue(it -> Set.of(ENDRING_I_REFUSJONSKRAV));
        var søknadperioderForBehandlingTidslinje = finnTidslinjeForRelevanteSøknadsperioder(behandlingReferanse).mapValue(it -> Set.of(SØKNAD_FRA_BRUKER));
        var personopplysningTidslinje = finnPersonopplysningTidslinje(behandlingReferanse, vilkårsperiode);
        var datoNyeReglerTidslinje = finnDatoNyeReglerTidslinje(behandlingReferanse, vilkårsperiode).mapValue(it -> Set.of(ENDRING_I_DATO_NYE_UTTAK_REGLER));
        var tidslinje = søknadperioderForBehandlingTidslinje
            .crossJoin(endringstidslinjeRefusjonskrav, StandardCombinators::union)
            .crossJoin(utvidetRevurderingPerioder, StandardCombinators::union)
            .crossJoin(personopplysningTidslinje, StandardCombinators::union)
            .crossJoin(datoNyeReglerTidslinje, StandardCombinators::union)
            .compress();

        tidslinje = fyllMellomromDersomKunHelg(tidslinje).compress();
        return tidslinje;
    }

    private <T> NavigableSet<DatoIntervallEntitet> finnPerioderRelevantForAktuellVilkårsperiode(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsperiode, LocalDateTimeline<T> tidslinje) {
        var originalBehandlingId = behandlingReferanse.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Forventer å finne original behandling"));
        var resultatperioder = new TreeSet<DatoIntervallEntitet>();
        var vilkårFraOriginalBehandling = getPeriodeTjeneste(behandlingReferanse)
            .utledFraDefinerendeVilkår(originalBehandlingId)
            .stream()
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toSet());
        resultatperioder.addAll(finnUttaksendringerSomOverlapperEllerErKantiKantMedPerioden(vilkårsperiode, tidslinje));
        resultatperioder.addAll(finnVilkårsperioderSomUmiddelbartEtterfølgerUttaksendringer(tidslinje, vilkårFraOriginalBehandling));
        return resultatperioder;
    }

    private LocalDateTimeline<Set<EndringsårsakUtbetaling>> finnBerørtePerioderPåBarnet(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsperiode) {
        var tidslinjeMedÅrsaker = pleietrengendeRevurderingPerioderTjeneste.utledBerørtePerioderPåPleietrengende(behandlingReferanse, getPeriodeTjeneste(behandlingReferanse).definerendeVilkår());
        return tidslinjeMedÅrsaker.filterValue(årsaker -> !årsaker.isEmpty()).intersection(vilkårsperiode.toLocalDateInterval()).mapValue(it -> Set.of(EndringsårsakUtbetaling.ENDRET_OPPLYSNINGER_OM_TILSYN_PLEIETRENGENDE));
    }

    private LocalDateTimeline<Set<EndringsårsakUtbetaling>> finnPersonopplysningTidslinje(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsperiode) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(behandlingReferanse, behandlingReferanse.getFagsakPeriode().getFomDato());

        var personopplysningSegmenter = new ArrayList<LocalDateSegment<Set<EndringsårsakUtbetaling>>>();
        var søkersDødsdato = personopplysningerAggregat.getSøker().getDødsdato();

        if (erDødRelevantForPeriode(vilkårsperiode, søkersDødsdato)) {
            personopplysningSegmenter.add(new LocalDateSegment<>(søkersDødsdato, vilkårsperiode.getTomDato(), Set.of(ENDRING_I_PERSONOPPLYSNING_SØKER)));
        }

        var pleietengendeDødsdato = personopplysningerAggregat.getPersonopplysning(behandlingReferanse.getPleietrengendeAktørId()).getDødsdato();
        if (erDødRelevantForPeriode(vilkårsperiode, pleietengendeDødsdato)) {
            personopplysningSegmenter.add(new LocalDateSegment<>(pleietengendeDødsdato, vilkårsperiode.getTomDato(), Set.of(ENDRING_I_PERSONOPPLYSNING_PLEIETRENGENDE)));

        }
        return new LocalDateTimeline<>(personopplysningSegmenter, StandardCombinators::union);
    }

    private static boolean erDødRelevantForPeriode(DatoIntervallEntitet vilkårsperiode, LocalDate pleietengendeDødsdato) {
        return pleietengendeDødsdato != null && vilkårsperiode.getTomDato().isAfter(pleietengendeDødsdato);
    }

    private LocalDateTimeline<Boolean> finnDatoNyeReglerTidslinje(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsperiode) {
        return finnTidslinjeForEndretRegelsett(behandlingReferanse, vilkårsperiode)
            .intersection(vilkårsperiode.toLocalDateInterval());
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForEndretRegelsett(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsperiode) {
        var forrigeDatoForNyeRegler = uttakNyeReglerRepository.finnForrigeDatoForNyeRegler(behandlingReferanse.getBehandlingId());
        var datoForNyeRegler = uttakNyeReglerRepository.finnDatoForNyeRegler(behandlingReferanse.getBehandlingId());
        if (forrigeDatoForNyeRegler.isEmpty() && datoForNyeRegler.isPresent() && !vilkårsperiode.getTomDato().isBefore(datoForNyeRegler.get())) {
            return new LocalDateTimeline<>(datoForNyeRegler.get(), vilkårsperiode.getTomDato(), TRUE);
        } else if (forrigeDatoForNyeRegler.isPresent() && datoForNyeRegler.isPresent() &&
            !forrigeDatoForNyeRegler.get().equals(datoForNyeRegler.get())) {
            var fom = forrigeDatoForNyeRegler.get().isBefore(datoForNyeRegler.get()) ? forrigeDatoForNyeRegler.get() : datoForNyeRegler.get();
            var tom = forrigeDatoForNyeRegler.get().isBefore(datoForNyeRegler.get()) ? datoForNyeRegler.get().minusDays(1) : forrigeDatoForNyeRegler.get().minusDays(1);
            return new LocalDateTimeline<>(fom, tom, TRUE);

        }
        return LocalDateTimeline.empty();
    }

    private static <T> LocalDateTimeline<T> fyllMellomromDersomKunHelg(LocalDateTimeline<T> tidslinje) {
        var mellomliggendeHelgUtleder = new MellomliggendeHelgUtleder();
        var mellomliggendePerioder = mellomliggendeHelgUtleder.beregnMellomliggendeHelg(tidslinje);
        tidslinje = tidslinje.combine(mellomliggendePerioder,
            kopierFraTilstøtendeVerdi(tidslinje), no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle.CROSS_JOIN);
        return tidslinje;
    }

    private static <T> LocalDateSegmentCombinator<T, Boolean, T> kopierFraTilstøtendeVerdi(LocalDateTimeline<T> tidslinje) {
        return (di, lhs, rhs) -> {
            if (lhs != null) {
                return lhs;
            }
            var forrigeSegment = tidslinje.getSegment(new LocalDateInterval(di.getFomDato().minusDays(1), di.getFomDato().minusDays(1)));
            if (forrigeSegment != null) {
                return new LocalDateSegment<>(di, forrigeSegment.getValue());
            }

            var nesteSegment = tidslinje.getSegment(new LocalDateInterval(di.getTomDato().plusDays(1), di.getTomDato().plusDays(1)));
            if (nesteSegment != null) {
                return new LocalDateSegment<>(di, nesteSegment.getValue());
            }

            return null;
        };
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

        var uttaksplan = finnUttaksplanEllerSimulering(behandlingReferanse);
        var originalUttakslpan = uttakRestKlient.hentUttaksplan(originalBehandling.getUuid(), true);


        var uttakTidslinje = lagTidslinje(uttaksplan);
        var originalUttakTidslinje = lagTidslinje(originalUttakslpan);

        var uttaksendringer = uttakTidslinje.combine(originalUttakTidslinje, TidslinjeUtil::forskjell, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        var resultatperioder = new ArrayList<DatoIntervallEntitet>();
        resultatperioder.addAll(finnUttaksendringerSomOverlapperEllerErKantiKantMedPerioden(periode, uttaksendringer));
        resultatperioder.addAll(finnVilkårsperioderSomUmiddelbartEtterfølgerUttaksendringer(uttaksendringer, vilkårFraOriginalBehandling));

        return new LocalDateTimeline<>(resultatperioder.stream().map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), TRUE)).toList(), StandardCombinators::alwaysTrueForMatch);
    }

    private Uttaksplan finnUttaksplanEllerSimulering(BehandlingReferanse behandlingReferanse) {
        var uttaksplan = uttakRestKlient.hentUttaksplan(behandlingReferanse.getBehandlingUuid(), true);
        if (uttaksplan == null && !utvidetUtlederEnabled) {
            return uttakRestKlient.simulerUttaksplan(mapInputTilUttakTjeneste.hentUtOgMapRequestUtenInntektsgradering(behandlingReferanse)).getSimulertUttaksplan();
        }
        return uttaksplan;
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
