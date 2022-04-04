package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.POST_VURDER_MEDISINSKVILKÅR;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.ArrayList;
import java.util.Collection;
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
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedlemskap;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;

@BehandlingStegRef(stegtype = POST_VURDER_MEDISINSKVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@ApplicationScoped
public class PostSykdomOgKontinuerligTilsynSteg implements BehandlingSteg {

    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;
    private AksjonspunktutlederForMedlemskap aksjonspunktutlederForMedlemskap;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;

    PostSykdomOgKontinuerligTilsynSteg() {
        // CDI
    }

    @Inject
    public PostSykdomOgKontinuerligTilsynSteg(BehandlingRepositoryProvider repositoryProvider,
                                              BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                              @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                              AksjonspunktutlederForMedlemskap aksjonspunktutlederForMedlemskap) {
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.aksjonspunktutlederForMedlemskap = aksjonspunktutlederForMedlemskap;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var vilkårsPerioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        final var perioderVurdertISykdom = utledPerioderVurdert(behandlingId, vilkårsPerioderTilVurderingTjeneste);

        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var oppdatertResultatBuilder = justerVilkårsperioderEtterSykdom(vilkårene, perioderVurdertISykdom, vilkårsPerioderTilVurderingTjeneste);

        vilkårResultatRepository.lagre(behandlingId, oppdatertResultatBuilder.build());

        reutledAksjonspunktForMedlemskap(kontekst);
        // Rydder bort grunnlag som ikke lenger er relevant siden perioden ikke skal vurderes
        // Disse blir da ikke lenger med til tilkjent ytelse, slik at det vedtaket blir inkosistent
        beregningPerioderGrunnlagRepository.ryddMotVilkår(behandlingId);

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

        return timeline.compress()
            .toSegments()
            .stream()
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    VilkårResultatBuilder justerVilkårsperioderEtterSykdom(Vilkårene vilkårene, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var avslåttePerioder = finnPerioderMedUtfall(vilkårene, perioderTilVurdering, perioderTilVurderingTjeneste, Utfall.IKKE_OPPFYLT);
        var innvilgedePerioder = finnPerioderMedUtfall(vilkårene, perioderTilVurdering, perioderTilVurderingTjeneste, Utfall.OPPFYLT);

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand());

        justerPeriodeForAndreVilkår(avslåttePerioder, innvilgedePerioder, resultatBuilder);

        return resultatBuilder;
    }

    private NavigableSet<VilkårPeriode> finnPerioderMedUtfall(Vilkårene vilkårene,
                                                              NavigableSet<DatoIntervallEntitet> perioderTilVurdering, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste, Utfall utfall) {
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();

        return definerendeVilkår.stream()
            .map(it -> vilkårene.getVilkår(it).orElseThrow())
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .filter(it -> Objects.equals(utfall, it.getUtfall()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private void justerPeriodeForAndreVilkår(NavigableSet<VilkårPeriode> avslåttePerioder, Set<VilkårPeriode> innvilgedePerioder, VilkårResultatBuilder resultatBuilder) {
        for (VilkårType vilkårType : Set.of(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårType.OPPTJENINGSVILKÅRET, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, VilkårType.MEDLEMSKAPSVILKÅRET)) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);
            var perioderSomSkalTilbakestilles = avslåttePerioder.stream()
                .map(VilkårPeriode::getPeriode)
                .filter(vilkårBuilder::harDataPåPeriode)
                .collect(Collectors.toCollection(TreeSet::new));
            if (!perioderSomSkalTilbakestilles.isEmpty()) {
                vilkårBuilder = vilkårBuilder.tilbakestill(perioderSomSkalTilbakestilles);
            }
            var innvilgetTidslinje = utledTidslinje(innvilgedePerioder, resultatBuilder.getKantIKantVurderer());
            for (DatoIntervallEntitet innvilgetPeriode : innvilgetTidslinje) {
                vilkårBuilder = vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(innvilgetPeriode)
                    .medPeriode(innvilgetPeriode));
            }
            resultatBuilder.leggTil(vilkårBuilder);
        }
    }

    private NavigableSet<DatoIntervallEntitet> utledTidslinje(Set<VilkårPeriode> innvilgedePerioder, KantIKantVurderer kantIKantVurderer) {
        DatoIntervallEntitet periode = null;
        var vilkårPerioder = new ArrayList<DatoIntervallEntitet>();

        for (VilkårPeriode vilkårPeriode : innvilgedePerioder) {
            if (periode == null) {
                periode = vilkårPeriode.getPeriode();
            } else if (kantIKantVurderer.erKantIKant(vilkårPeriode.getPeriode(), periode)) {
                periode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), vilkårPeriode.getTom());
            } else {
                vilkårPerioder.add(periode);
                periode = vilkårPeriode.getPeriode();
            }
        }
        if (periode != null) {
            vilkårPerioder.add(periode);
        }
        return adjustAndCompress(vilkårPerioder);
    }

    private NavigableSet<DatoIntervallEntitet> adjustAndCompress(ArrayList<DatoIntervallEntitet> vilkårPerioder) {
        var segmenter = vilkårPerioder.stream()
            .map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), true))
            .toList();

        return new LocalDateTimeline<>(segmenter)
            .compress()
            .toSegments()
            .stream()
            .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

}
