package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
        final var perioder = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.OPPTJENINGSVILKÅRET); // OK
        final var perioderMedlemskap = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.MEDLEMSKAPSVILKÅRET);

        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        var oppdatertResultatBuilder = justerVilkårsperioderEtterSykdom(vilkårene, perioder, perioderMedlemskap);

        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdatertResultatBuilder.build());

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    VilkårResultatBuilder justerVilkårsperioderEtterSykdom(Vilkårene vilkårene, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, NavigableSet<DatoIntervallEntitet> medlemskapsPerioderTilVurdering) {
        var avslåttePerioder = avslåttePerioder(vilkårene, perioderTilVurdering);
        var innvilgedePerioder = finnInnvilgedePerioder(vilkårene, perioderTilVurdering);

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .medMaksMellomliggendePeriodeAvstand(perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand());

        //justerPeriodeForMedlemskap(innvilgedePerioder, resultatBuilder, medlemskapsPerioderTilVurdering);
        justerPeriodeForOpptjeningOgBeregning(avslåttePerioder, innvilgedePerioder, resultatBuilder);

        return resultatBuilder;
    }

    private Set<VilkårPeriode> avslåttePerioder(Vilkårene vilkårene,
                                                NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var s1 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR).orElseThrow().getPerioder().stream();
        var s2 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR).orElseThrow().getPerioder().stream();
        return Stream.concat(s1, s2)
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getUtfall()))
            .collect(Collectors.toSet());
    }

    private Set<VilkårPeriode> finnInnvilgedePerioder(Vilkårene vilkårene,
                                                NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var s1 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR).orElseThrow().getPerioder().stream();
        var s2 = vilkårene.getVilkår(VilkårType.MEDISINSKEVILKÅR_18_ÅR).orElseThrow().getPerioder().stream();
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
            for (VilkårPeriode innvilgetPeriode : innvilgedePerioder) {
                vilkårBuilder = vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(innvilgetPeriode.getPeriode())
                    .medPeriode(innvilgetPeriode.getPeriode())
                    .medUtfall(Utfall.IKKE_VURDERT));
            }
            for (VilkårPeriode avslåttPeriode : avslåttePerioder) {
                vilkårBuilder = vilkårBuilder.tilbakestill(avslåttPeriode.getPeriode());
            }
            resultatBuilder.leggTil(vilkårBuilder);
        }
    }
}
