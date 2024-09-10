package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.POST_VURDER_MEDISINSKVILKÅR;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
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
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedlemskap;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.OverstyrUttakTjeneste;

@BehandlingStegRef(value = POST_VURDER_MEDISINSKVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@ApplicationScoped
public class FastsettSkjæringstidspunkterForYtelseSteg implements BehandlingSteg {

    public static final Set<VilkårType> AVHENGIGE_VILKÅR = Set.of(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårType.OPPTJENINGSVILKÅRET, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, VilkårType.MEDLEMSKAPSVILKÅRET);
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;

    private OverstyrUttakTjeneste overstyrUttakTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private AksjonspunktutlederForMedlemskap aksjonspunktutlederForMedlemskap;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårTjeneste vilkårTjeneste;
    private BehandlingRepository behandlingRepository;

    FastsettSkjæringstidspunkterForYtelseSteg() {
        // CDI
    }

    @Inject
    public FastsettSkjæringstidspunkterForYtelseSteg(BehandlingRepositoryProvider repositoryProvider,
                                                     BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                                     OverstyrUttakTjeneste overstyrUttakTjeneste,
                                                     @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                     AksjonspunktutlederForMedlemskap aksjonspunktutlederForMedlemskap, VilkårTjeneste vilkårTjeneste) {
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.overstyrUttakTjeneste = overstyrUttakTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.aksjonspunktutlederForMedlemskap = aksjonspunktutlederForMedlemskap;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var vilkårsPerioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        final var perioderVurdertISykdom = utledPerioderVurdert(behandlingId, vilkårsPerioderTilVurderingTjeneste);

        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var oppdatertResultatBuilder = justerVilkårsperioderEtterDefinerendeVilkår(vilkårene, perioderVurdertISykdom, vilkårsPerioderTilVurderingTjeneste);

        vilkårResultatRepository.lagre(behandlingId, oppdatertResultatBuilder.build());

        reutledAksjonspunktForMedlemskap(kontekst);
        // Rydder bort grunnlag som ikke lenger er relevant siden perioden ikke skal vurderes
        // Disse blir da ikke lenger med til tilkjent ytelse, slik at det vedtaket blir inkosistent
        beregningPerioderGrunnlagRepository.ryddMotVilkår(behandlingId);
        ryddVilkårsperioderSomIkkeLengerVurderes(behandling, finnAllePerioderMedUtfall(vilkårene, vilkårsPerioderTilVurderingTjeneste), perioderVurdertISykdom);
        overstyrUttakTjeneste.ryddMotVilkår(BehandlingReferanse.fra(behandling));
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void reutledAksjonspunktForMedlemskap(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);

        if (behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP)) {

            var aksjonspunktResultats = aksjonspunktutlederForMedlemskap.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));
            if (aksjonspunktResultats.isEmpty()) {
                behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP).avbryt();
                behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
            }
        }
    }

    private NavigableSet<DatoIntervallEntitet> utledPerioderVurdert(Long behandlingId, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjenester) {
        var timeline = new LocalDateTimeline<Boolean>(List.of());
        var definerendeVilkår = perioderTilVurderingTjenester.definerendeVilkår();

        for (VilkårType vilkårType : definerendeVilkår) {
            var perioder = perioderTilVurderingTjenester.utled(behandlingId, vilkårType);
            var periodeTidslinje = new LocalDateTimeline<>(perioder.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).collect(Collectors.toList()));

            timeline = timeline.combine(periodeTidslinje, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return TidslinjeUtil.tilDatoIntervallEntiteter(timeline.compress());
    }

    VilkårResultatBuilder justerVilkårsperioderEtterDefinerendeVilkår(Vilkårene vilkårene, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var innvilgedePerioder = finnVurdertePerioderMedUtfall(vilkårene, perioderTilVurdering, perioderTilVurderingTjeneste);

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand());

        justerPeriodeForAndreVilkår(innvilgedePerioder, resultatBuilder);

        return resultatBuilder;
    }

    private LocalDateTimeline<Boolean> finnVurdertePerioderMedUtfall(Vilkårene vilkårene,
                                                                     NavigableSet<DatoIntervallEntitet> perioderTilVurdering, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();
        LocalDateTimeline<Boolean> tidslinje = LocalDateTimeline.empty();

        for (VilkårType vilkårType : definerendeVilkår) {
            var segmenter = vilkårene.getVilkår(vilkårType).orElseThrow()
                .getPerioder()
                .stream()
                .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
                .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), Objects.equals(Utfall.OPPFYLT, it.getGjeldendeUtfall())))
                .collect(Collectors.toCollection(TreeSet::new));
            tidslinje = tidslinje.combine(new LocalDateTimeline<>(segmenter), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return tidslinje.compress();
    }

    private void justerPeriodeForAndreVilkår(LocalDateTimeline<Boolean> tidslinje, VilkårResultatBuilder resultatBuilder) {
        for (VilkårType vilkårType : AVHENGIGE_VILKÅR) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);

            // Fjerner avslåtte perioder
            var perioderSomSkalTilbakestilles = tidslinje.filterValue(it -> !it)
                .stream()
                .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
                .filter(vilkårBuilder::harDataPåPeriode)
                .filter(p -> erIkkeBareKantIKantMellomrom(tidslinje, p, resultatBuilder.getKantIKantVurderer()))
                .collect(Collectors.toCollection(TreeSet::new));
            if (!perioderSomSkalTilbakestilles.isEmpty()) {
                vilkårBuilder = vilkårBuilder.tilbakestill(perioderSomSkalTilbakestilles);
            }

            // Legger til innvilgede perioder
            var innvilgetTidslinje = tidslinje.filterValue(it -> it);
            var innvilgendePerioder = innvilgetTidslinje.combine(Hjelpetidslinjer.utledHullSomMåTettes(innvilgetTidslinje, resultatBuilder.getKantIKantVurderer()), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN)
                .stream().map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval())).collect(Collectors.toCollection(TreeSet::new));
            for (DatoIntervallEntitet innvilgetPeriode : innvilgendePerioder) {
                vilkårBuilder = vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(innvilgetPeriode)
                    .medPeriode(innvilgetPeriode));
            }

            resultatBuilder.leggTil(vilkårBuilder);
        }
    }

    // Kopierer originalt vilkårsresultat for perioder som ikke lenger vurderes i behandlingen
    // Dersom periodene ikke fantes originalt klippes de bort
    // TODO: Det ligger også en håndtering av dette i PreconditionBeregningSteg (se GjenopprettPerioderSomIkkeVurderesTjeneste) for bg-vilkårt. Optimalt sett burde denne vurderingen ligge så nærme som mulig vurdering av vilkåret, og bruke ein felles funksjonalitet.
    private void ryddVilkårsperioderSomIkkeLengerVurderes(Behandling behandling, LocalDateTimeline<Utfall> vilkårutfallTidslinje, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        if (behandling.getOriginalBehandlingId().isPresent()) {
            for (VilkårType vilkårType : AVHENGIGE_VILKÅR) {
                var resultat = vilkårTjeneste.gjenopprettVilkårsutfallForPerioderSomIkkeVurderes(BehandlingReferanse.fra(behandling), vilkårType, perioderTilVurdering, true);
                var fjernetPerioder = resultat.fjernetPerioder();
                var intersects = vilkårutfallTidslinje.filterValue(it -> it.equals(Utfall.OPPFYLT)).intersects(TidslinjeUtil.tilTidslinjeKomprimert(fjernetPerioder));
                if (intersects) {
                    throw new IllegalStateException("Kan ikke fjerne innvilget periode");
                }
            }
        }
    }

    private LocalDateTimeline<Utfall> finnAllePerioderMedUtfall(Vilkårene vilkårene, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();
        LocalDateTimeline<Utfall> tidslinje = LocalDateTimeline.empty();

        for (VilkårType vilkårType : definerendeVilkår) {
            var segmenter = vilkårene.getVilkår(vilkårType).orElseThrow()
                .getPerioder()
                .stream()
                .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), it.getGjeldendeUtfall()))
                .collect(Collectors.toCollection(TreeSet::new));
            tidslinje = tidslinje.combine(new LocalDateTimeline<>(segmenter), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return tidslinje.compress();
    }

    private static boolean erIkkeBareKantIKantMellomrom(LocalDateTimeline<Boolean> tidslinje, DatoIntervallEntitet p, KantIKantVurderer kantIKantVurderer) {
        return Hjelpetidslinjer.utledHullSomMåTettes(tidslinje.disjoint(p.toLocalDateInterval()), kantIKantVurderer).intersection(p.toLocalDateInterval()).isEmpty();
    }

}
