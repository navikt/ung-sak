package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@BehandlingStegRef(kode = "POST_MEDISINSK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PostSykdomOgKontinuerligTilsynSteg implements BehandlingSteg {

    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    PostSykdomOgKontinuerligTilsynSteg() {
        // CDI
    }

    @Inject
    public PostSykdomOgKontinuerligTilsynSteg(BehandlingRepositoryProvider repositoryProvider,
                                              @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        final var perioderVurdertISykdom = utledPerioderVurdert(behandlingId);

        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var oppdatertResultatBuilder = justerVilkårsperioderEtterSykdom(vilkårene, perioderVurdertISykdom);

        vilkårResultatRepository.lagre(behandlingId, oppdatertResultatBuilder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private NavigableSet<DatoIntervallEntitet> utledPerioderVurdert(Long behandlingId) {
        var perioderUnder18 = perioderTilVurderingTjeneste.utled(behandlingId, VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var perioder18OgOver = perioderTilVurderingTjeneste.utled(behandlingId, VilkårType.MEDISINSKEVILKÅR_18_ÅR);

        var perioderUnder = new LocalDateTimeline<>(perioderUnder18.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).collect(Collectors.toList()));
        var perioderOver = new LocalDateTimeline<>(perioder18OgOver.stream().map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true)).collect(Collectors.toList()));

        return perioderUnder.combine(perioderOver, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .compress()
            .toSegments()
            .stream()
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    VilkårResultatBuilder justerVilkårsperioderEtterSykdom(Vilkårene vilkårene, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var avslåttePerioder = avslåttePerioder(vilkårene, perioderTilVurdering);
        var innvilgedePerioder = finnInnvilgedePerioder(vilkårene, perioderTilVurdering);

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand());

        justerPeriodeForOpptjeningOgBeregning(avslåttePerioder, innvilgedePerioder, resultatBuilder);

        return resultatBuilder;
    }

    private Set<VilkårPeriode> avslåttePerioder(Vilkårene vilkårene,
                                                NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var s1 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR).orElseThrow()
            .getPerioder()
            .stream();
        var s2 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR).orElseThrow()
            .getPerioder()
            .stream();
        return Stream.concat(s1, s2)
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getUtfall()))
            .collect(Collectors.toSet());
    }

    private Set<VilkårPeriode> finnInnvilgedePerioder(Vilkårene vilkårene,
                                                      NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var s1 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR).orElseThrow()
            .getPerioder()
            .stream();
        var s2 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR).orElseThrow()
            .getPerioder()
            .stream();
        return Stream.concat(s1, s2)
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .filter(it -> Utfall.OPPFYLT.equals(it.getUtfall()))
            .collect(Collectors.toSet());
    }

    private void justerPeriodeForOpptjeningOgBeregning(Set<VilkårPeriode> avslåttePerioder, Set<VilkårPeriode> innvilgedePerioder, VilkårResultatBuilder resultatBuilder) {
        if (innvilgedePerioder.isEmpty()) {
            return;
        }

        for (VilkårType vilkårType : Set.of(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårType.OPPTJENINGSVILKÅRET, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, VilkårType.MEDLEMSKAPSVILKÅRET)) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);
            for (VilkårPeriode avslåttPeriode : avslåttePerioder) {
                if (vilkårBuilder.harDataPåPeriode(avslåttPeriode.getPeriode())) {
                    vilkårBuilder = vilkårBuilder.tilbakestill(avslåttPeriode.getPeriode());
                }
            }
            for (VilkårPeriode innvilgetPeriode : innvilgedePerioder) {
                vilkårBuilder = vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(innvilgetPeriode.getPeriode())
                    .medPeriode(innvilgetPeriode.getPeriode()));
            }
            resultatBuilder.leggTil(vilkårBuilder);
        }
    }
}
