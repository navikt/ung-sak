package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static java.lang.Boolean.TRUE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.ENDRET_FORDELING_PROSESS_TRIGGER;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.ENDRING_FRA_ANNEN_OMSORGSPERSON_PROSESS_TRIGGER;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.ENDRING_I_DATO_NYE_UTTAK_REGLER;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.ENDRING_I_PERSONOPPLYSNING_PLEIETRENGENDE;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.ENDRING_I_PERSONOPPLYSNING_SØKER;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.ENDRING_I_REFUSJONSKRAV;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.GJENOPPTAR_UTSATT_BEHANDLING_PROSESS_TRIGGER;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning.EndringsårsakUtbetaling.SØKNAD_FRA_BRUKER;

import java.time.DayOfWeek;
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
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.ErEndringIRefusjonskravVurderer;
import no.nav.k9.sak.domene.opptjening.MellomliggendeHelgUtleder;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.EndretUtbetalingPeriodeutleder;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.PleietrengendeRevurderingPerioderTjeneste;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@ApplicationScoped
public class PleiepengerEndretUtbetalingPeriodeutleder implements EndretUtbetalingPeriodeutleder {
    public static final Set<BehandlingÅrsakType> ENDRET_FORDELING_ÅRSAKER = Set.of(BehandlingÅrsakType.RE_ENDRET_FORDELING, BehandlingÅrsakType.RE_GJENOPPTAR_UTSATT_BEHANDLING, BehandlingÅrsakType.RE_ENDRING_FRA_ANNEN_OMSORGSPERSON);
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private ProsessTriggereRepository prosessTriggereRepository;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private UttakNyeReglerRepository uttakNyeReglerRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private PleietrengendeRevurderingPerioderTjeneste pleietrengendeRevurderingPerioderTjeneste;
    private ErEndringIRefusjonskravVurderer erEndringIRefusjonskravVurderer;

    public PleiepengerEndretUtbetalingPeriodeutleder() {
    }

    @Inject
    public PleiepengerEndretUtbetalingPeriodeutleder(@Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                                                     ProsessTriggereRepository prosessTriggereRepository,
                                                     SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                     UttakNyeReglerRepository uttakNyeReglerRepository,
                                                     PersonopplysningTjeneste personopplysningTjeneste,
                                                     PleietrengendeRevurderingPerioderTjeneste pleietrengendeRevurderingPerioderTjeneste,
                                                     ErEndringIRefusjonskravVurderer erEndringIRefusjonskravVurderer) {
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.uttakNyeReglerRepository = uttakNyeReglerRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.pleietrengendeRevurderingPerioderTjeneste = pleietrengendeRevurderingPerioderTjeneste;
        this.erEndringIRefusjonskravVurderer = erEndringIRefusjonskravVurderer;
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
        var tidslinje = finnÅrsakstidslinje(behandlingReferanse, vilkårsperiode);
        var perioderMedRelevantEndring = finnPerioderRelevantForAktuellVilkårsperiode(behandlingReferanse, vilkårsperiode, tidslinje);
        // Grunnet problematikk rundt flipping av status fra aktiv til ikke-yrkesaktiv (se https://jira.adeo.no/browse/TSFF-278)
        return inkluderSammeUkeOgEtterFølgendeUkerFraEndringsdato(vilkårsperiode, perioderMedRelevantEndring);
    }

    private static TreeSet<DatoIntervallEntitet> inkluderSammeUkeOgEtterFølgendeUkerFraEndringsdato(DatoIntervallEntitet vilkårsperiode, NavigableSet<DatoIntervallEntitet> perioderMedRelevantEndring) {
        var fomDato = perioderMedRelevantEndring.stream().map(DatoIntervallEntitet::getFomDato)
            .filter(fom -> !fom.isAfter(vilkårsperiode.getTomDato()))
            .min(Comparator.naturalOrder())
            .map(fom -> fom.getDayOfWeek().equals(DayOfWeek.SUNDAY) || fom.getDayOfWeek().equals(DayOfWeek.SATURDAY) ? fom : førsteDagIUken(fom))
            .map(fom -> fom.isBefore(vilkårsperiode.getFomDato()) ? vilkårsperiode.getFomDato() : fom);
        return fomDato.map(localDate -> new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(localDate, vilkårsperiode.getTomDato())))).orElseGet(TreeSet::new);
    }

    private static LocalDate førsteDagIUken(LocalDate d) {
        return d.minusDays(d.getDayOfWeek().getValue() - 1);
    }

    public LocalDateTimeline<Set<EndringsårsakUtbetaling>> finnÅrsakstidslinje(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet vilkårsperiode) {
        var utvidetRevurderingPerioder = finnBerørtePerioderPåBarnet(behandlingReferanse, vilkårsperiode);
        var endringstidslinjeRefusjonskrav = erEndringIRefusjonskravVurderer.finnEndringstidslinjeForRefusjon(behandlingReferanse, vilkårsperiode).mapValue(it -> Set.of(ENDRING_I_REFUSJONSKRAV));
        var søknadperioderForBehandlingTidslinje = finnTidslinjeForRelevanteSøknadsperioder(behandlingReferanse).mapValue(it -> Set.of(SØKNAD_FRA_BRUKER));
        var personopplysningTidslinje = finnPersonopplysningTidslinje(behandlingReferanse, vilkårsperiode);
        var datoNyeReglerTidslinje = finnDatoNyeReglerTidslinje(behandlingReferanse, vilkårsperiode).mapValue(it -> Set.of(ENDRING_I_DATO_NYE_UTTAK_REGLER));
        var prosesstriggerTidslinje = finnTidslinjeFraProsessTriggere(behandlingReferanse);
        var tidslinje = søknadperioderForBehandlingTidslinje
            .crossJoin(endringstidslinjeRefusjonskrav, StandardCombinators::union)
            .crossJoin(prosesstriggerTidslinje, StandardCombinators::union)
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
        resultatperioder.addAll(finnEndringerSomOverlapperEllerErKantiKantMedPerioden(vilkårsperiode, tidslinje));
        resultatperioder.addAll(finnVilkårsperioderSomUmiddelbartEtterfølgerEndringer(tidslinje, vilkårFraOriginalBehandling));
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
        var originalDatoForNyeRegler = behandlingReferanse.getOriginalBehandlingId().flatMap(uttakNyeReglerRepository::finnDatoForNyeRegler);
        var datoForNyeRegler = uttakNyeReglerRepository.finnDatoForNyeRegler(behandlingReferanse.getBehandlingId());
        if (originalDatoForNyeRegler.isEmpty() && datoForNyeRegler.isPresent() && !vilkårsperiode.getTomDato().isBefore(datoForNyeRegler.get())) {
            return new LocalDateTimeline<>(datoForNyeRegler.get(), vilkårsperiode.getTomDato(), TRUE);
        } else if (originalDatoForNyeRegler.isPresent() && datoForNyeRegler.isPresent() &&
            !originalDatoForNyeRegler.get().equals(datoForNyeRegler.get())) {
            var fom = originalDatoForNyeRegler.get().isBefore(datoForNyeRegler.get()) ? originalDatoForNyeRegler.get() : datoForNyeRegler.get();
            var tom = originalDatoForNyeRegler.get().isBefore(datoForNyeRegler.get()) ? datoForNyeRegler.get().minusDays(1) : originalDatoForNyeRegler.get().minusDays(1);
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

    private LocalDateTimeline<Set<EndringsårsakUtbetaling>> finnTidslinjeFraProsessTriggere(BehandlingReferanse behandlingReferanse) {
        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var perioderFraTriggere = prosessTriggere.stream().flatMap(it -> it.getTriggere().stream())
            .filter(it -> ENDRET_FORDELING_ÅRSAKER.contains(it.getÅrsak()))
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), Set.of(mapTilÅrsak(it.getÅrsak()))))
            .collect(Collectors.toSet());
        return new LocalDateTimeline<>(perioderFraTriggere, StandardCombinators::union);
    }

    private EndringsårsakUtbetaling mapTilÅrsak(BehandlingÅrsakType årsak) {
        return switch (årsak) {
            case RE_ENDRET_FORDELING -> ENDRET_FORDELING_PROSESS_TRIGGER;
            case RE_ENDRING_FRA_ANNEN_OMSORGSPERSON -> ENDRING_FRA_ANNEN_OMSORGSPERSON_PROSESS_TRIGGER;
            case RE_GJENOPPTAR_UTSATT_BEHANDLING -> GJENOPPTAR_UTSATT_BEHANDLING_PROSESS_TRIGGER;
            default -> throw new IllegalArgumentException("Fikk BehandlingÅrsakType som ikke er mappet til endringsårsak: " + årsak);
        };
    }

    private LocalDateTimeline<Boolean> finnTidslinjeForRelevanteSøknadsperioder(BehandlingReferanse behandlingReferanse) {
        var relevantePerioder = søknadsperiodeTjeneste.utledPeriode(behandlingReferanse.getBehandlingId(), false);
        var søknadsperioderForBehandling = relevantePerioder
            .stream()
            .map(p -> new LocalDateSegment<>(p.getFomDato(), p.getTomDato(), TRUE)).toList();
        return new LocalDateTimeline<>(søknadsperioderForBehandling, StandardCombinators::coalesceLeftHandSide);
    }

    private List<DatoIntervallEntitet> finnVilkårsperioderSomUmiddelbartEtterfølgerEndringer(LocalDateTimeline<?> uttaksendringer, Set<DatoIntervallEntitet> vilkårFraOrginalBehandling) {
        var kantIKantVurderer = new PåTversAvHelgErKantIKantVurderer();
        return vilkårFraOrginalBehandling.stream()
            .filter(gammelVilkårsperiode -> uttaksendringer.stream()
                .anyMatch(uttaksendring -> uttaksendring.getTom().isBefore(gammelVilkårsperiode.getTomDato()) && kantIKantVurderer.erKantIKant(gammelVilkårsperiode, tilIntervall(uttaksendring))))
            .toList();
    }

    private DatoIntervallEntitet tilIntervall(LocalDateSegment<?> s) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom());
    }

    private <V> NavigableSet<DatoIntervallEntitet> finnEndringerSomOverlapperEllerErKantiKantMedPerioden(DatoIntervallEntitet vilkårsperiode, LocalDateTimeline<V> differanse) {
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


}
